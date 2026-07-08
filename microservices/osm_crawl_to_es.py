#!/usr/bin/env python3
"""
Crawler OSM → Elasticsearch (mode sans Kafka)
Réplique exactement la logique du OsmCrawlerService.java mais écrit
directement dans l'index crawler-index d'Elasticsearch.

Usage:
    python3 osm_crawl_to_es.py

Villes et types configurés dans CITIES et OSM_TYPES ci-dessous.
"""

import requests
import json
import time
import hashlib
import sys
from datetime import datetime, timezone

ES_URL    = "http://localhost:9200"
ES_INDEX  = "crawler-index"
OVERPASS  = "https://overpass-api.de/api/interpreter"
HEADERS   = {"User-Agent": "YowYob-Crawler/1.0 (projet academique)"}

# ── Config identique à application.yml ────────────────────────────────────
CITIES = [
    {"name": "Yaoundé",   "lat": 3.8480,  "lng": 11.5021, "radius": 15000},
    {"name": "Douala",    "lat": 4.0511,  "lng": 9.7679,  "radius": 15000},
    {"name": "Bafoussam", "lat": 5.4737,  "lng": 10.4179, "radius": 10000},
]

OSM_TYPES = [
    "restaurant", "pharmacy", "supermarket",
    "bank", "hospital", "cafe", "hotel",
    "school", "fuel",
]

# Mapping type → tag OSM (identique à typeToOsmTag dans le Java)
def osm_tag(t):
    if t in ("shop", "supermarket", "market"):
        return "shop", t
    return "amenity", t

# ── Requête Overpass (identique à buildOverpassQuery dans le Java) ─────────
def build_query(place_type, lat, lng, radius):
    tag_key, tag_val = osm_tag(place_type)
    return f"""
[out:json][timeout:30];
(
  node["{tag_key}"="{tag_val}"](around:{radius},{lat},{lng});
  way["{tag_key}"="{tag_val}"](around:{radius},{lat},{lng});
  relation["{tag_key}"="{tag_val}"](around:{radius},{lat},{lng});
);
out center tags;
"""

# ── Fetch depuis Overpass avec retry ──────────────────────────────────────
def fetch_osm(place_type, lat, lng, radius, retries=3):
    query = build_query(place_type, lat, lng, radius)
    for attempt in range(1, retries + 1):
        try:
            r = requests.post(OVERPASS, data={"data": query}, headers=HEADERS, timeout=40)
            if r.status_code == 429:
                wait = 15 * attempt
                print(f"  ⚠️  Rate limit Overpass — attente {wait}s (tentative {attempt}/{retries})")
                time.sleep(wait)
                continue
            r.raise_for_status()
            elements = r.json().get("elements", [])
            time.sleep(3)  # pause obligatoire après requête réussie
            return elements
        except Exception as e:
            print(f"  ❌ Erreur tentative {attempt}/{retries} : {e}")
            if attempt < retries:
                time.sleep(10)
    return []

# ── Normalisation d'un élément OSM → document ES ──────────────────────────
def to_es_doc(element, city_name, place_type):
    tags = element.get("tags", {})
    name = tags.get("name", "").strip()
    if not name:
        return None  # on n'indexe pas les POI sans nom

    # Coordonnées (node = directement, way/relation = center)
    center = element.get("center", {})
    lat = element.get("lat") or center.get("lat")
    lng = element.get("lon") or center.get("lon")
    if not lat or not lng:
        return None

    # ID stable basé sur le source OSM id
    osm_id = f"osm_{element['type']}_{element['id']}"

    # Adresse
    parts = [tags.get("addr:housenumber",""), tags.get("addr:street","")]
    address = " ".join(p for p in parts if p).strip() or tags.get("addr:full","")

    # Catégorie lisible
    category = (
        tags.get("amenity") or
        tags.get("shop") or
        tags.get("tourism") or
        place_type
    ).upper()

    return {
        "id":          osm_id,
        "title":       name,
        "description": f"{category.capitalize()} situé à {city_name}.",
        "category":    category,
        "city":        city_name,
        "quartier":    tags.get("addr:suburb") or tags.get("addr:district") or None,
        "type":        "shop",
        "serviceType": "businessbook",
        "source":      "CRAWLER",
        "phone":       tags.get("phone") or tags.get("contact:phone") or None,
        "website":     tags.get("website") or tags.get("contact:website") or None,
        "openingHours":tags.get("opening_hours") or None,
        "street":      address or None,
        "latitude":    lat,
        "longitude":   lng,
        "location":    {"lat": lat, "lon": lng},   # geo_point pour near-me
        "crawledAt":   datetime.now(timezone.utc).isoformat(),
    }

# ── Indexation dans Elasticsearch ─────────────────────────────────────────
def index_doc(doc):
    url = f"{ES_URL}/{ES_INDEX}/_doc/{doc['id']}"
    r = requests.put(url, json=doc, headers={"Content-Type": "application/json"}, timeout=10)
    return r.json().get("result") in ("created", "updated")

# ── Main ───────────────────────────────────────────────────────────────────
def main():
    print(f"\n🚀 Démarrage du crawler OSM → Elasticsearch ({ES_INDEX})")
    print(f"   Villes  : {[c['name'] for c in CITIES]}")
    print(f"   Types   : {OSM_TYPES}\n")

    total_indexed = 0
    total_skipped = 0

    for city in CITIES:
        print(f"\n📍 Ville : {city['name']} ({city['lat']}, {city['lng']}) — rayon {city['radius']}m")

        for place_type in OSM_TYPES:
            print(f"   🔍 Type : {place_type} ...", end=" ", flush=True)

            elements = fetch_osm(place_type, city["lat"], city["lng"], city["radius"])
            print(f"{len(elements)} éléments récupérés", end="")

            indexed = 0
            for el in elements:
                doc = to_es_doc(el, city["name"], place_type)
                if doc is None:
                    total_skipped += 1
                    continue
                if index_doc(doc):
                    indexed += 1
                    total_indexed += 1

            print(f" → {indexed} indexés ✅")

    print(f"\n✅ Crawl terminé — {total_indexed} documents indexés, {total_skipped} ignorés (sans nom/coords)")
    print(f"   Elasticsearch : {ES_URL}/{ES_INDEX}\n")

if __name__ == "__main__":
    # Vérifier la connexion ES avant de commencer
    try:
        r = requests.get(f"{ES_URL}/_cluster/health", timeout=5)
        status = r.json().get("status", "unknown")
        print(f"✅ Elasticsearch connecté — status: {status}")
    except Exception as e:
        print(f"❌ Impossible de joindre Elasticsearch sur {ES_URL} : {e}")
        sys.exit(1)

    main()
