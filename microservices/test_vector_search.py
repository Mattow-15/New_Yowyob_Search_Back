#!/usr/bin/env python3
"""
Test de validation de la recherche vectorielle.
Lance après reindex_with_vectors.py.
"""
import requests, json, sys

ES_URL    = "http://localhost:9200"
EMBED_URL = "http://localhost:8000"
TARGET_INDEX = "yowyob-search-v3"

QUERIES = [
    ("je veux manger",          "FOOD → restaurants sémantiques"),
    ("j'ai faim",               "FOOD intent paraphrase"),
    ("je suis malade",          "HEALTH → hôpital/pharmacie"),
    ("retirer de l'argent",     "FINANCE → banque/ATM"),
    ("acheter des médicaments", "HEALTH shopping"),
    ("faire le plein",          "FUEL → station essence"),
]

def embed(text):
    r = requests.post(f"{EMBED_URL}/embed", json={"text": text}, timeout=10)
    return r.json()["vector"]

def knn_search(vector, k=5):
    body = {
        "knn": {
            "field": "textVector",
            "query_vector": vector,
            "k": k,
            "num_candidates": 100
        },
        "_source": ["title", "collection", "tenantId"]
    }
    r = requests.post(f"{ES_URL}/{TARGET_INDEX}/_search", json=body,
                      headers={"Content-Type": "application/json"}, timeout=10)
    hits = r.json().get("hits", {}).get("hits", [])
    return [(h["_source"].get("title", "?"), round(h.get("_score", 0), 3)) for h in hits]

def main():
    print("=" * 60)
    print("  TEST DE LA RECHERCHE VECTORIELLE kNN")
    print("=" * 60)

    # Santé du service d'embeddings
    try:
        h = requests.get(f"{EMBED_URL}/health", timeout=5).json()
        print(f"\n✅ Embeddings: {h['status']} | dims={h['dimensions']}")
    except Exception as e:
        print(f"❌ Service embeddings injoignable: {e}")
        sys.exit(1)

    # Vérification de l'index
    count = requests.post(
        f"{ES_URL}/{TARGET_INDEX}/_count",
        json={"query": {"exists": {"field": "textVector"}}},
        headers={"Content-Type": "application/json"}, timeout=10
    ).json().get("count", 0)
    print(f"✅ {count} documents avec textVector dans {TARGET_INDEX}\n")

    for query, label in QUERIES:
        print(f"🔍 « {query} »  ({label})")
        try:
            vec = embed(query)
            results = knn_search(vec)
            if results:
                for title, score in results[:3]:
                    print(f"   {score:.3f}  {title}")
            else:
                print("   (aucun résultat)")
        except Exception as e:
            print(f"   ❌ Erreur: {e}")
        print()

if __name__ == "__main__":
    main()
