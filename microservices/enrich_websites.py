#!/usr/bin/env python3
"""
Enrichit le champ `website` dans crawler-index de façon SÛRE :
1. Remet à null tous les websites sauf ceux déjà présents dans OSM
2. Applique le dictionnaire manuel uniquement pour les grandes enseignes
   dont le nom est une correspondance EXACTE (pas de propagation générique)
"""
import re, json, requests

ES = "http://localhost:9200"
INDEX = "crawler-index"

# ── Dictionnaire : enseigne connue → site officiel ────────────────────────
# Clés = variantes de noms EXACTES (minuscules, normalisées)
# N'ajouter QUE des enseignes dont le site est certain.
BRAND_SITES: dict[str, str] = {
    # Banques Cameroun
    "ecobank":                          "https://www.ecobank.com/cm",
    "bicec":                            "https://www.bicec.com",
    "afriland first bank":              "https://www.afrilandfirstbank.com",
    "afriland":                         "https://www.afrilandfirstbank.com",
    "société générale cameroun":        "https://www.societegenerale.cm",
    "societe generale cameroun":        "https://www.societegenerale.cm",
    "société générale":                 "https://www.societegenerale.cm",
    "scb cameroun":                     "https://www.scb.cm",
    "bgfi bank":                        "https://www.bgfibank.com",
    "bgfi banque":                      "https://www.bgfibank.com",
    "uba":                              "https://www.ubagroup.com/cm",
    "standard chartered":               "https://www.sc.com/cm",
    "nfc bank":                         "https://www.nfcbank.cm",
    "cbc bank":                         "https://www.cbcbank.cm",
    "banque atlantique":                "https://www.banque-atlantique.com",
    "advans cameroon":                  "https://www.advanscmr.com",
    "express union":                    "https://www.expressunion.cm",
    # Mobile money / transfert
    "western union":                    "https://www.westernunion.com",
    "moneygram":                        "https://www.moneygram.com",
    "mtn mobile money point":           "https://www.mtn.cm",
    # Energie
    "total":                            "https://totalenergies.com/cm",
    "totalenergies":                    "https://totalenergies.com/cm",
    "eneo":                             "https://www.eneo.cm",
    "aes sonel":                        "https://www.eneo.cm",
    "camwater":                         "https://www.camwater.cm",
    "camtel":                           "https://www.camtel.cm",
    # Telecom
    "orange":                           "https://www.orange.cm",
    "mtn":                              "https://www.mtn.cm",
    # Autres
    "canal+":                           "https://www.canalplus-afrique.com",
    "dstv":                             "https://www.dstv.com",
    "sabc":                             "https://www.sabcbrasseries.com",
    "sosucam":                          "https://www.sosucam.com",
    "chococam":                         "https://www.chococam.com",
    "camair-co":                        "https://www.camair-co.cm",
    "camair":                           "https://www.camair-co.cm",
}

def normalize(s: str) -> str:
    return re.sub(r'\s+', ' ', (s or '').lower().strip())

def scroll_all(query=None):
    body = {"size": 500, "_source": ["title", "website"], "query": query or {"match_all": {}}}
    r = requests.post(f"{ES}/{INDEX}/_search?scroll=5m", json=body,
                      headers={"Content-Type": "application/json"}, timeout=30)
    data = r.json()
    scroll_id = data["_scroll_id"]
    hits = data["hits"]["hits"]
    while hits:
        yield hits
        r = requests.post(f"{ES}/_search/scroll",
                          json={"scroll": "5m", "scroll_id": scroll_id},
                          headers={"Content-Type": "application/json"}, timeout=30)
        data = r.json()
        scroll_id = data.get("_scroll_id", scroll_id)
        hits = data["hits"]["hits"]

def bulk_set(ops: list[dict]):
    if not ops:
        return
    lines = []
    for op in ops:
        lines.append(json.dumps({"update": {"_index": INDEX, "_id": op["id"]}}))
        lines.append(json.dumps({"doc": {"website": op["value"]}}))
    body = "\n".join(lines) + "\n"
    requests.post(f"{ES}/_bulk", data=body.encode(),
                  headers={"Content-Type": "application/x-ndjson"}, timeout=60)

# ── Étape 1 : sauvegarder les websites OSM originaux ─────────────────────
print("📥 Sauvegarde des websites OSM originaux…")
osm_originals: dict[str, str] = {}  # doc_id → website
for hits in scroll_all({"exists": {"field": "website"}}):
    for h in hits:
        w = h["_source"].get("website") or ""
        if w and "@" not in w:  # ignorer les emails
            if not w.startswith("http"):
                w = "https://" + w
            osm_originals[h["_id"]] = w

print(f"   {len(osm_originals)} docs avec website OSM valide")

# ── Étape 2 : effacer tous les websites ───────────────────────────────────
print("🗑  Remise à null de tous les websites…")
cleared = 0
ops = []
for hits in scroll_all():
    for h in hits:
        ops.append({"id": h["_id"], "value": None})
    if len(ops) >= 500:
        bulk_set(ops); cleared += len(ops); ops = []
if ops:
    bulk_set(ops); cleared += len(ops)
print(f"   {cleared} docs remis à null")

# ── Étape 3 : restaurer les OSM originaux ─────────────────────────────────
print("✅ Restauration des websites OSM originaux…")
ops = [{"id": doc_id, "value": w} for doc_id, w in osm_originals.items()]
for i in range(0, len(ops), 200):
    bulk_set(ops[i:i+200])
print(f"   {len(ops)} docs restaurés")

# ── Étape 4 : appliquer le dictionnaire sur les docs sans website ─────────
print("🔧 Application du dictionnaire manuel (correspondances exactes)…")
matched = 0
ops = []
for hits in scroll_all({"bool": {"must_not": {"exists": {"field": "website"}}}}):
    for h in hits:
        title = normalize(h["_source"].get("title") or "")
        website = BRAND_SITES.get(title)
        # Essai par préfixe strict (max 3 mots du début)
        if not website:
            words = title.split()
            for n in range(min(3, len(words)), 1, -1):
                prefix = " ".join(words[:n])
                if prefix in BRAND_SITES:
                    website = BRAND_SITES[prefix]
                    break
        if website:
            ops.append({"id": h["_id"], "value": website})
    if len(ops) >= 200:
        bulk_set(ops); matched += len(ops); ops = []
if ops:
    bulk_set(ops); matched += len(ops)
print(f"   {matched} docs enrichis via dictionnaire")

# ── Bilan ─────────────────────────────────────────────────────────────────
total_with = requests.post(f"{ES}/{INDEX}/_count",
    json={"query": {"exists": {"field": "website"}}},
    headers={"Content-Type": "application/json"}).json().get("count", 0)
total_all = requests.get(f"{ES}/{INDEX}/_count").json().get("count", 0)
print(f"\n📊 Final : {total_with}/{total_all} docs ont un website fiable")
