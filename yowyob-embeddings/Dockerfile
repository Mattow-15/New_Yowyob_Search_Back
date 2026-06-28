# yowyob-embeddings — image du micro-service d'embeddings (FastAPI + sentence-transformers).
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Pré-télécharge le modèle dans l'image pour un démarrage rapide et hors-ligne du conteneur.
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')"

COPY . .

EXPOSE 8000

# Healthcheck aligné sur l'endpoint /health (statut "starting" tant que le modèle charge).
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/health').status==200 else 1)" || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
