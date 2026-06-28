# yowyob-search

Service de recherche **autonome et multi-tenant**, intégrable à **n'importe quel projet** via HTTP.
Le projet pousse ses documents (ingestion) et interroge (recherche) ; le service possède son propre
Elasticsearch. Tout langage parlant HTTP peut l'utiliser.

📖 **Intégrateurs** : [`docs/DEVELOPERS.md`](docs/DEVELOPERS.md) — **Mainteneurs** : [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Authentification — déléguée au kernel
yowyob-search **ne gère pas de clés** : il valide les identifiants d'un **clientApplication du kernel**
en interrogeant le kernel (`GET /api/client-applications/me`). Un clientApplication créé dans le
kernel marche donc **immédiatement**, sans config ni redémarrage ici.
- En-têtes `X-Client-Id` + `X-Api-Key` : ceux d'un clientApplication kernel.
- En-tête `X-Tenant-Id` : isolation multi-tenant **obligatoire** sur tous les appels `/api/**`.

## Ingestion

Upsert d'un document (objet JSON arbitraire) dans une `collection` (type logique libre) :
```bash
curl -X PUT https://search.yowyob.com/api/index/products/p-42 \
  -H "X-Client-Id: $CLIENT_ID" -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{"name":"Café Arabica","sku":"CAF-001","price":1500}'
```

Upsert en lot (chaque élément porte un champ `id`) :
```bash
curl -X POST https://search.yowyob.com/api/index/products/_bulk \
  -H "X-Client-Id: $CLIENT_ID" -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT" -H "Content-Type: application/json" \
  -d '[{"id":"p-1","name":"A"},{"id":"p-2","name":"B"}]'
```

Suppression :
```bash
curl -X DELETE https://search.yowyob.com/api/index/products/p-42 \
  -H "X-Client-Id: $CLIENT_ID" -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT"
```

## Recherche
```bash
curl "https://search.yowyob.com/api/search?q=arabica&collection=products&page=0&size=20" \
  -H "X-Client-Id: $CLIENT_ID" -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT"
```
Réponse :
```json
{ "count": 1, "results": [ { "collection": "products", "id": "p-42",
  "title": "Café Arabica", "source": { "name": "Café Arabica", "sku": "CAF-001", "price": 1500 } } ] }
```
`collection` est optionnel (omis = recherche tous types). Résultats toujours scopés au tenant.

## Recherche sémantique (hybride)
En plus du lexical « search-as-you-type » (edge-ngram), le service fait de la **recherche
sémantique** : chaque document est vectorisé par le micro-service [`yowyob-embeddings`](yowyob-embeddings/)
(modèle multilingue 384 dims) et chaque requête combine **lexical + kNN vectoriel**. Une recherche
`q=ordinateur portable` remonte aussi un `"laptop"` même sans mot-clé commun.

- Transparent : **même endpoint** `GET /api/search`, aucune option à passer.
- **Tolérant aux pannes** : si `yowyob-embeddings` est indisponible (ou `SEARCH_EMBEDDING_ENABLED=false`),
  le service **dégrade automatiquement** vers le lexical seul.
- Réglages via env : `SEARCH_EMBEDDING_ENABLED`, `EMBEDDING_SERVICE_URL`, `SEARCH_EMBEDDING_K`,
  `SEARCH_EMBEDDING_NUM_CANDIDATES`, `SEARCH_EMBEDDING_BOOST`.

> ⚠️ L'index passe de `-v2` à `-v3` (ajout du champ vectoriel). Au déploiement, un index neuf est
> créé : les données existantes doivent être **ré-poussées** par les clients pour bénéficier du
> sémantique (le lexical, lui, fonctionne dès le premier push).

## Intégration type
Le **backend** de chaque projet (jamais le navigateur) :
1. à chaque création/màj d'une entité → `PUT /api/index/{collection}/{id}` ;
2. à chaque suppression → `DELETE /api/index/{collection}/{id}` ;
3. pour la barre de recherche → `GET /api/search?q=...`.

## Déploiement
Spring Boot + Elasticsearch dédié. CI GitHub Actions → image GHCR → déploiement SSH, exposé par
Traefik sur `search.yowyob.com`. Variables runtime dans le `.env` serveur (`KERNEL_BASE_URL`). Auth déléguée au kernel : plus de `SEARCH_API_KEYS`.
