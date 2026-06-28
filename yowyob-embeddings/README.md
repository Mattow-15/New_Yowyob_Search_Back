# yowyob-embeddings

Micro-service de génération d'embeddings vectoriels pour **yowyob-search** (recherche sémantique).

- Modèle : `paraphrase-multilingual-MiniLM-L12-v2` (FR/EN, **384 dimensions**).
- Stack : FastAPI + sentence-transformers (torch CPU-only).
- Sans état, un seul rôle : transformer un texte en vecteur.

## Endpoints

| Méthode | Chemin    | Description |
|---------|-----------|-------------|
| `GET`   | `/health` | `{"status":"healthy","model":...,"dimensions":384}` une fois le modèle chargé. |
| `POST`  | `/embed`  | Corps `{"text":"..."}` → `{"vector":[...],"dimensions":384}`. |

## Lancer en local

```bash
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

## Intégration

`yowyob-search` appelle ce service via `EmbeddingClient` (URL `yowyob.search.embedding.url`).
Si le service est indisponible, yowyob-search **dégrade automatiquement** vers la recherche
lexicale (edge-ngram) — l'embeddings n'est jamais un point de panne dur.

> ⚠️ La dimension (384) doit rester cohérente entre ce modèle, le champ `text_vector`
> d'Elasticsearch (`SearchDoc`) et `yowyob.search.embedding.dimensions`.
