#!/usr/bin/env python3
"""
Ré-indexation vectorielle : crawler-index → yowyob-search-v3
============================================================
1. Crée l'index yowyob-search-v3 avec mapping dense_vector (384 dims, cosine)
   et l'analyzer edge-ngram, exactement comme SearchDoc.java l'attend.
2. Lit tous les documents de crawler-index.
3. Pour chaque document, appelle le service yowyob-embeddings pour générer
   le vecteur sémantique du texte (title + description + category + city).
4. Indexe le document dans yowyob-search-v3.

Usage:
    python3 reindex_with_vectors.py

Prérequis:
    - Elasticsearch sur localhost:9200
    - yowyob-embeddings sur localhost:8000 (uvicorn main:app)
    - Index crawler-index peuplé
"""

import requests
import json
import time
import sys
from datetime import datetime, timezone

ES_URL        = "http://localhost:9200"
EMBED_URL     = "http://localhost:8000"
SOURCE_INDEX  = "crawler-index"
TARGET_INDEX  = "yowyob-search-v3"
TENANT_ID     = "yowyob"
COLLECTION    = "places"
BATCH_SIZE    = 50

# ── Mapping identique à SearchDoc.java ────────────────────────────────────────
INDEX_SETTINGS = {
  "settings": {
    "analysis": {
      "filter": {
        "edge_ngram_filter": {
          "type": "edge_ngram",
          "min_gram": 2,
          "max_gram": 20
        }
      },
      "analyzer": {
        "edge_ngram_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "edge_ngram_filter"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "_class":      {"type": "keyword"},
      "id":          {"type": "keyword"},
      "tenantId":    {"type": "keyword"},
      "collection":  {"type": "keyword"},
      "externalId":  {"type": "keyword"},
      "title":       {"type": "text", "analyzer": "edge_ngram_analyzer", "search_analyzer": "standard"},
      "content":     {"type": "text", "analyzer": "edge_ngram_analyzer", "search_analyzer": "standard"},
      "source":      {"type": "keyword"},
      "textVector":  {
        "type":       "dense_vector",
        "dims":       384,
        "index":      True,
        "similarity": "cosine"
      },
      "location":    {"type": "geo_point"},
      "indexedAt":   {"type": "date", "format": "strict_date_optional_time"}
    }
  }
}

def create_index():
    r = requests.delete(f"{ES_URL}/{TARGET_INDEX}", timeout=10)
    r = requests.put(f"{ES_URL}/{TARGET_INDEX}", json=INDEX_SETTINGS,
                     headers={"Content-Type": "application/json"}, timeout=10)
    if r.status_code not in (200, 201):
        print(f"❌ Erreur création index: {r.text[:200]}")
        sys.exit(1)
    print(f"✅ Index {TARGET_INDEX} créé (dense_vector 384 dims, cosine)")

def embed(text: str) -> list[float] | None:
    try:
        r = requests.post(f"{EMBED_URL}/embed", json={"text": text}, timeout=10)
        if r.status_code == 200:
            return r.json().get("vector")
    except Exception as e:
        print(f"  ⚠️  Embed error: {e}")
    return None

def scroll_all_docs():
    """Scroll sur crawler-index pour récupérer tous les documents."""
    r = requests.post(
        f"{ES_URL}/{SOURCE_INDEX}/_search?scroll=2m",
        json={"query": {"match_all": {}}, "size": BATCH_SIZE},
        headers={"Content-Type": "application/json"}, timeout=30
    )
    data = r.json()
    scroll_id = data.get("_scroll_id")
    hits = data.get("hits", {}).get("hits", [])
    total = data.get("hits", {}).get("total", {}).get("value", 0)
    return total, scroll_id, hits

def scroll_next(scroll_id: str):
    r = requests.post(
        f"{ES_URL}/_search/scroll",
        json={"scroll": "2m", "scroll_id": scroll_id},
        headers={"Content-Type": "application/json"}, timeout=30
    )
    data = r.json()
    return data.get("_scroll_id"), data.get("hits", {}).get("hits", [])

def build_doc(src: dict, vector: list[float] | None) -> dict:
    """Construit le document SearchDoc à partir d'un doc crawler-index."""
    osm_id = src.get("id", "")
    title   = src.get("title", "") or ""
    desc    = src.get("description", "") or ""
    cat     = src.get("category", "") or ""
    city    = src.get("city", "") or ""

    # content = concaténation des champs texte (comme DocumentMapper.embeddingText)
    content_parts = [p for p in [title, desc, cat, city, src.get("street")] if p]
    content = " | ".join(content_parts)

    # ID SearchDoc = tenantId:collection:externalId
    doc_id = f"{TENANT_ID}:{COLLECTION}:{osm_id}"

    doc = {
        "_class": "com.yowyob.search.domain.SearchDoc",
        "id":          doc_id,
        "tenantId":    TENANT_ID,
        "collection":  COLLECTION,
        "externalId":  osm_id,
        "title":       title,
        "content":     content,
        "source":      src.get("source", "CRAWLER"),
        "indexedAt":   datetime.now(timezone.utc).isoformat(),
    }

    if vector:
        doc["textVector"] = vector

    lat = src.get("latitude") or (src.get("location") or {}).get("lat")
    lon = src.get("longitude") or (src.get("location") or {}).get("lon")
    if lat is not None and lon is not None:
        doc["location"] = {"lat": lat, "lon": lon}

    return doc_id, doc

def bulk_index(batch: list[tuple[str, dict]]):
    lines = []
    for doc_id, doc in batch:
        lines.append(json.dumps({"index": {"_index": TARGET_INDEX, "_id": doc_id}}))
        lines.append(json.dumps(doc))
    body = "\n".join(lines) + "\n"
    r = requests.post(f"{ES_URL}/_bulk", data=body,
                      headers={"Content-Type": "application/x-ndjson"}, timeout=30)
    resp = r.json()
    errors = [i for i in resp.get("items", []) if i.get("index", {}).get("error")]
    return len(resp.get("items", [])) - len(errors), len(errors)

def main():
    print("\n🔍 Vérification des prérequis...")

    # Elasticsearch
    try:
        r = requests.get(f"{ES_URL}/_cluster/health", timeout=5)
        print(f"  ✅ Elasticsearch: {r.json().get('status')}")
    except Exception as e:
        print(f"  ❌ Elasticsearch injoignable: {e}")
        sys.exit(1)

    # Embedding service
    try:
        r = requests.get(f"{EMBED_URL}/health", timeout=5)
        h = r.json()
        print(f"  ✅ Embeddings: {h.get('status')} | modèle: {h.get('model')} | dims: {h.get('dimensions')}")
    except Exception as e:
        print(f"  ❌ Service embeddings injoignable sur {EMBED_URL}: {e}")
        sys.exit(1)

    # Création de l'index
    print(f"\n🏗️  Création de l'index {TARGET_INDEX}...")
    create_index()

    # Scroll + vectorisation + indexation
    print(f"\n⚙️  Début de la ré-indexation depuis {SOURCE_INDEX}...")
    total, scroll_id, hits = scroll_all_docs()
    print(f"   {total} documents à traiter\n")

    indexed = 0
    errors  = 0
    batch_n = 0
    t0      = time.time()

    while hits:
        batch_n += 1
        batch = []
        for hit in hits:
            src  = hit.get("_source", {})
            text = " ".join(filter(None, [
                src.get("title"), src.get("description"),
                src.get("category"), src.get("city")
            ]))
            vector = embed(text) if text.strip() else None
            doc_id, doc = build_doc(src, vector)
            batch.append((doc_id, doc))

        ok, err = bulk_index(batch)
        indexed += ok
        errors  += err

        elapsed = time.time() - t0
        rate    = indexed / elapsed if elapsed > 0 else 0
        eta     = (total - indexed) / rate if rate > 0 else 0
        pct     = indexed / total * 100 if total > 0 else 0
        print(f"  batch {batch_n:3d} | {indexed:4d}/{total} ({pct:.0f}%) | "
              f"{rate:.1f} docs/s | ETA {eta:.0f}s | erreurs: {errors}")

        scroll_id, hits = scroll_next(scroll_id)

    elapsed = time.time() - t0
    print(f"\n✅ Ré-indexation terminée en {elapsed:.0f}s")
    print(f"   {indexed} documents indexés dans {TARGET_INDEX}")
    print(f"   {errors} erreurs")
    if indexed > 0:
        vec_count = requests.post(
            f"{ES_URL}/{TARGET_INDEX}/_count",
            json={"query": {"exists": {"field": "textVector"}}},
            headers={"Content-Type": "application/json"}, timeout=10
        ).json().get("count", "?")
        print(f"   {vec_count} documents avec vecteur textVector (kNN actif)")

if __name__ == "__main__":
    main()
