import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

# Configuration du logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Modèle global (chargé au démarrage)
model = None
MODEL_NAME = 'paraphrase-multilingual-MiniLM-L12-v2'

@asynccontextmanager
async def lifespan(app: FastAPI):
    global model
    logger.info(f"Chargement du modèle d'embeddings: {MODEL_NAME}...")
    try:
        model = SentenceTransformer(MODEL_NAME)
        logger.info("Modèle chargé avec succès !")
    except Exception as e:
        logger.error(f"Erreur lors du chargement du modèle : {e}")
        raise e
    yield
    # Nettoyage à l'arrêt si nécessaire
    logger.info("Arrêt du service d'embeddings.")

app = FastAPI(
    title="YowYob Embeddings API",
    description="Microservice de génération d'embeddings vectoriels pour YowYob Search Engine",
    version="1.0.0",
    lifespan=lifespan
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
    return {"status": "healthy", "model": MODEL_NAME}

@app.post("/embed", response_model=EmbedResponse)
async def generate_embedding(request: EmbedRequest):
    if model is None:
        raise HTTPException(status_code=503, detail="Le modèle n'est pas encore prêt.")
    
    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="Le texte ne peut pas être vide.")
    
    try:
        # Générer l'embedding
        embedding = model.encode(request.text.strip())
        
        # Convertir le numpy array en liste Python native pour la sérialisation JSON
        vector_list = embedding.tolist()
        
        return EmbedResponse(
            vector=vector_list,
            dimensions=len(vector_list)
        )
    except Exception as e:
        logger.error(f"Erreur lors de la génération de l'embedding : {e}")
        raise HTTPException(status_code=500, detail=str(e))
