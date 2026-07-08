"""
yowyob-embeddings — micro-service de génération d'embeddings vectoriels.

Convertit un texte en vecteur dense (384 dimensions) via sentence-transformers.
Appelé par yowyob-search (EmbeddingClient) pour :
  - enrichir chaque document indexé d'un champ `text_vector` (recherche sémantique kNN) ;
  - vectoriser la requête utilisateur au moment de la recherche.

Le service est volontairement minimal et sans état : un seul modèle chargé au démarrage,
deux endpoints (`/health`, `/embed`). Tout échec côté yowyob-search est dégradé en
recherche lexicale (edge-ngram) — l'embeddings n'est donc jamais un point de panne dur.
"""
import asyncio
import logging
from contextlib import asynccontextmanager
from concurrent.futures import ThreadPoolExecutor

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

MODEL_NAME = "paraphrase-multilingual-MiniLM-L12-v2"
model: SentenceTransformer | None = None
_executor = ThreadPoolExecutor(max_workers=2)


def _load_and_warm(name: str) -> SentenceTransformer:
    """Charge le modèle et exécute un appel de warmup pour JIT-compiler torch."""
    logger.info("Chargement du modèle %s ...", name)
    m = SentenceTransformer(name)
    logger.info("Modèle chargé — warmup JIT en cours ...")
    m.encode("warmup")
    logger.info("Modèle prêt (dims=%s).", m.get_sentence_embedding_dimension())
    return m


@asynccontextmanager
async def lifespan(app: FastAPI):
    global model
    loop = asyncio.get_event_loop()
    # Chargement + warmup dans un thread séparé pour ne pas bloquer l'event loop
    model = await loop.run_in_executor(_executor, _load_and_warm, MODEL_NAME)
    yield
    logger.info("Arrêt du service d'embeddings.")


app = FastAPI(
    title="YowYob Embeddings API",
    description="Micro-service de génération d'embeddings vectoriels pour yowyob-search.",
    version="1.0.0",
    lifespan=lifespan,
)


class EmbedRequest(BaseModel):
    text: str


class EmbedResponse(BaseModel):
    vector: list[float]
    dimensions: int


@app.get("/health")
async def health_check():
    if model is None:
        return {"status": "starting", "model": MODEL_NAME}
    return {"status": "healthy", "model": MODEL_NAME, "dimensions": model.get_sentence_embedding_dimension()}


@app.post("/embed", response_model=EmbedResponse)
async def generate_embedding(request: EmbedRequest):
    if model is None:
        raise HTTPException(status_code=503, detail="Le modèle n'est pas encore prêt.")
    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="Le texte ne peut pas être vide.")
    try:
        loop = asyncio.get_event_loop()
        vector = await loop.run_in_executor(_executor, lambda: model.encode(request.text.strip()).tolist())
        return EmbedResponse(vector=vector, dimensions=len(vector))
    except Exception as exc:  # noqa: BLE001
        logger.error("Erreur de génération d'embedding: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc)) from exc
