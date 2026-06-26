# yowyob-search

Service de recherche **autonome et multi-tenant**, intégrable à **n'importe quel projet** via HTTP.
Le projet pousse ses documents (ingestion) et interroge (recherche) ; le service possède son propre
Elasticsearch. Aucune dépendance au kernel — tout langage parlant HTTP peut l'utiliser.

## Authentification
- En-tête `X-Api-Key` : une clé par projet intégrateur (configurée via `SEARCH_API_KEYS` côté serveur).
- En-tête `X-Tenant-Id` : isolation multi-tenant **obligatoire** sur tous les appels `/api/**`.

## Ingestion

Upsert d'un document (objet JSON arbitraire) dans une `collection` (type logique libre) :
```bash
curl -X PUT https://search.yowyob.com/api/index/products/p-42 \
  -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{"name":"Café Arabica","sku":"CAF-001","price":1500}'
```

Upsert en lot (chaque élément porte un champ `id`) :
```bash
curl -X POST https://search.yowyob.com/api/index/products/_bulk \
  -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT" -H "Content-Type: application/json" \
  -d '[{"id":"p-1","name":"A"},{"id":"p-2","name":"B"}]'
```

Suppression :
```bash
curl -X DELETE https://search.yowyob.com/api/index/products/p-42 \
  -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT"
```

## Recherche
```bash
curl "https://search.yowyob.com/api/search?q=arabica&collection=products&page=0&size=20" \
  -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT"
```
Réponse :
```json
{ "count": 1, "results": [ { "collection": "products", "id": "p-42",
  "title": "Café Arabica", "source": { "name": "Café Arabica", "sku": "CAF-001", "price": 1500 } } ] }
```
`collection` est optionnel (omis = recherche tous types). Résultats toujours scopés au tenant.

## Intégration type
Le **backend** de chaque projet (jamais le navigateur) :
1. à chaque création/màj d'une entité → `PUT /api/index/{collection}/{id}` ;
2. à chaque suppression → `DELETE /api/index/{collection}/{id}` ;
3. pour la barre de recherche → `GET /api/search?q=...`.

## Déploiement
Spring Boot + Elasticsearch dédié. CI GitHub Actions → image GHCR → déploiement SSH, exposé par
Traefik sur `search.yowyob.com`. Variables runtime dans le `.env` serveur (`SEARCH_API_KEYS`).
