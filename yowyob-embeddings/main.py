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
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# Modèle multilingue (FR/EN) compact : 384 dimensions. DOIT correspondre à `dims` du
# champ `text_vector` côté Elasticsearch (cf. SearchDoc) et à yowyob.search.embedding.dimensions.
MODEL_NAME = "paraphrase-multilingual-MiniLM-L12-v2"
model: SentenceTransformer | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global model
    logger.info("Chargement du modèle d'embeddings: %s ...", MODEL_NAME)
    model = SentenceTransformer(MODEL_NAME)
    logger.info("Modèle chargé (dimensions=%s).", model.get_sentence_embedding_dimension())
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
        vector = model.encode(request.text.strip()).tolist()
        return EmbedResponse(vector=vector, dimensions=len(vector))
    except Exception as exc:  # noqa: BLE001 — on remonte une 500 explicite au client
        logger.error("Erreur de génération d'embedding: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc)) from exc
