#!/usr/bin/env python3
"""
Peuple le champ `embedding` (384-dim cosine) dans crawler-index.
Le backend Java ProductDocument cherche ce champ pour le kNN sémantique.
"""
import requests, json, time, sys

ES_URL    = "http://localhost:9200"
EMBED_URL = "http://localhost:8000"
INDEX     = "crawler-index"
BATCH     = 50

def embed(text: str) -> list[float] | None:
    try:
        r = requests.post(f"{EMBED_URL}/embed", json={"text": text}, timeout=15)
        return r.json()["vector"]
    except Exception as e:
        print(f"  ⚠ embed error: {e}")
        return None

def scroll_all():
    r = requests.post(
        f"{ES_URL}/{INDEX}/_search?scroll=5m",
        json={"size": BATCH, "query": {"match_all": {}},
              "_source": ["id", "title", "category", "description"]},
        headers={"Content-Type": "application/json"}, timeout=30)
    data = r.json()
    scroll_id = data["_scroll_id"]
    hits = data["hits"]["hits"]
    total = data["hits"]["total"]["value"]
    yield hits, total
    while hits:
        r = requests.post(
            f"{ES_URL}/_search/scroll",
            json={"scroll": "5m", "scroll_id": scroll_id},
            headers={"Content-Type": "application/json"}, timeout=30)
        data = r.json()
        scroll_id = data.get("_scroll_id", scroll_id)
        hits = data["hits"]["hits"]
        if hits:
            yield hits, total

def bulk_update(ops: list[dict]):
    lines = []
    for op in ops:
        lines.append(json.dumps({"update": {"_index": INDEX, "_id": op["id"]}}))
        lines.append(json.dumps({"doc": {"embedding": op["vector"]}}))
    body = "\n".join(lines) + "\n"
    r = requests.post(f"{ES_URL}/_bulk",
                      data=body.encode(),
                      headers={"Content-Type": "application/x-ndjson"}, timeout=60)
    result = r.json()
    errors = sum(1 for item in result.get("items", [])
                 if item.get("update", {}).get("error"))
    return errors

def main():
    # Santé du service
    try:
        h = requests.get(f"{EMBED_URL}/health", timeout=5).json()
        print(f"✅ Embeddings: {h['status']} | model: {h.get('model')} | dims: {h.get('dimensions')}")
    except Exception as e:
        print(f"❌ Service embeddings injoignable: {e}")
        sys.exit(1)

    # Combien de docs déjà vectorisés ?
    already = requests.post(f"{ES_URL}/{INDEX}/_count",
        json={"query": {"exists": {"field": "embedding"}}},
        headers={"Content-Type": "application/json"}).json().get("count", 0)
    total_count = requests.get(f"{ES_URL}/{INDEX}/_count").json().get("count", 0)
    print(f"📊 {already}/{total_count} docs déjà vectorisés dans {INDEX}")

    done = 0
    errors_total = 0
    start = time.time()

    for hits, total in scroll_all():
        ops = []
        for hit in hits:
            src = hit["_source"]
            text = f"{src.get('title','')} {src.get('category','')} {src.get('description','')}"
            vec = embed(text.strip())
            if vec:
                ops.append({"id": hit["_id"], "vector": vec})

        if ops:
            errs = bulk_update(ops)
            errors_total += errs

        done += len(hits)
        elapsed = time.time() - start
        rate = done / elapsed if elapsed > 0 else 0
        eta = int((total - done) / rate) if rate > 0 else 0
        print(f"  {done:5d}/{total} ({int(100*done/total)}%) | {rate:.1f} docs/s | ETA {eta}s | erreurs: {errors_total}")

    print(f"\n✅ Terminé en {int(time.time()-start)}s")
    print(f"   {done} docs traités | {errors_total} erreurs")

if __name__ == "__main__":
    main()
