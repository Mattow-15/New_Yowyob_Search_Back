# Architecture & maintenance — yowyob-search

Doc pour **les personnes qui maintiennent yowyob-search** (pas pour les intégrateurs — eux, voir
[`DEVELOPERS.md`](DEVELOPERS.md)). Objectif : comprendre comment le service est construit, **ce qui
a changé récemment (auth)**, comment le déployer et comment l'étendre.

---

## 1. Vue d'ensemble

yowyob-search est un **microservice de recherche autonome, multi-tenant**, accessible en HTTP par
n'importe quel projet. Il possède son **propre Elasticsearch dédié** et ne partage aucune base avec
le kernel.

```
  Projet intégrateur (backend)                       yowyob-search
  ────────────────────────────                       ─────────────
   create/update/delete  ──PUT/DELETE /api/index──►  IndexController ─► IndexService ─► ES dédié
   barre de recherche    ──GET /api/search───────►   SearchController ─► SearchService ─► ES dédié

   Auth de CHAQUE appel /api/** :
   X-Client-Id + X-Api-Key  ──(validés en live)──►   KernelAuthWebFilter ──GET /me──►  KERNEL
```

- **Stack** : Spring Boot 3.4.1 **WebFlux** + Spring Data **Elasticsearch réactif**, Java 21, fat jar Maven.
- **ES dédié** : sidecar `search-elasticsearch` (8.15.3), volume `search-es-data`, réseau privé
  `yowyob-search-net`. Le service est aussi sur le réseau partagé `yowyob` (Traefik + accès aux
  autres conteneurs, **dont le kernel**).
- **Exposition** : Traefik `https://search.yowyob.com` (Let's Encrypt).
- **OpenAPI/Swagger** : springdoc (`OpenApiConfig`). UI sur `/swagger-ui.html`, spec sur
  `/v3/api-docs` — **hors `/api/**`**, donc ouverts (le filtre d'auth n'intercepte que `/api/**`).
  Le bouton *Authorize* propose les 3 en-têtes (`X-Client-Id`/`X-Api-Key`/`X-Tenant-Id`).

---

## 2. ⚠️ Changement majeur (2026-06-26) — l'auth est maintenant **déléguée au kernel**

### Ce qui a changé et pourquoi
**Avant** : yowyob-search tenait sa propre liste de clés statiques (`SEARCH_API_KEYS` dans le `.env`).
Ajouter un client = éditer le `.env` + **redémarrer** le service. Lourd et désynchronisé du kernel.

**Exigence** (demande explicite) : *« l'ajout d'un clientApplication dans le kernel doit le faire
automatiquement dans yowyob-search et plus question de recharger yowyob-search ».*

**Après** : yowyob-search **ne gère plus aucune clé**. Pour chaque appel `/api/**`, il prend les
en-têtes `X-Client-Id` + `X-Api-Key` et les **valide en direct auprès du kernel** via
`GET /api/client-applications/me` (200 = valide). Résultat retenu dans un **cache mémoire** quelques
minutes pour ne pas marteler le kernel.

→ **Un clientApplication créé dans le kernel fonctionne immédiatement ici. Zéro config, zéro reload.**
C'est cohérent avec tout le reste de la plateforme : le **kernel est la source unique des identités**.

### Conséquences à connaître (maintenance)
- **Dépendance runtime au kernel** : si le kernel est injoignable, les couples non encore en cache
  sont rejetés (401). Les couples déjà validés restent acceptés jusqu'à expiration du cache.
- yowyob-search doit pouvoir **joindre le kernel** sur le réseau Docker `yowyob`
  (`KERNEL_BASE_URL`, défaut `http://kernel-core-kernel-layer-1:8080`).
- **Aucun secret** propre à yowyob-search à provisionner/roter. `SEARCH_API_KEYS` n'existe plus.

### Fichiers concernés
| Fichier | Rôle |
|---|---|
| `config/KernelAuthWebFilter.java` | `WebFilter` : extrait `X-Client-Id`/`X-Api-Key`, cache, sinon appelle `/me` du kernel ; 401 sinon. `/actuator/**` laissé ouvert. |
| `config/KernelAuthProperties.java` | `@ConfigurationProperties(prefix="yowyob.search.auth")` : `enabled`, `kernelBaseUrl`, `cacheTtlSeconds`. |
| `resources/application.yml` | bloc `yowyob.search.auth.*` (variables `SEARCH_AUTH_ENABLED`, `KERNEL_BASE_URL`, `SEARCH_AUTH_CACHE_TTL`). |
| ~~`config/SearchProperties.java`~~, ~~`config/ApiKeyWebFilter.java`~~ | **supprimés** (ancienne auth statique). |

### Comportement du cache
Clé de cache = `clientId + " " + apiKey` → instant d'expiration. Implémentation simple
(`ConcurrentHashMap`, pas d'éviction active : les entrées expirées sont juste ignorées et
ré-écrasées). Volume attendu faible (poignée de clients). Si un jour beaucoup de clés distinctes :
remplacer par un cache borné (Caffeine) — point d'extension identifié.

---

## 3. Le kernel POUSSE ses données ici (consolidation)

Depuis la même date, le kernel **n'a plus de recherche globale interne** : il pousse toutes ses
entités métier vers yowyob-search. C'est **un intégrateur comme un autre**, qui se trouve être le kernel.

- Côté kernel : un `GlobalSearchIndexConsumer` écoute tous les événements métier et appelle
  `PUT /api/index/{collection}/{id}` (ou `DELETE` sur `*_DELETED`), best-effort.
- Auth du kernel vers nous : il utilise le clientApplication **`yowyob-search`** (son `X-Client-Id`
  + son secret) — que notre filtre valide via `/me`, exactement comme tout le monde.
- `collection` = type d'agrégat en minuscules (`product`, `organization`, `user_account`…).

> À retenir : **aucun couplage de code** avec le kernel ici. On ne connaît que son URL (pour valider
> les identifiants). Le kernel nous parle uniquement via l'API HTTP publique `/api/index`.

---

## 4. Composants internes

| Couche | Classe | Rôle |
|---|---|---|
| Domaine | `domain/SearchDoc.java` | Document ES (`@Document index=yowyob-search-v1`). `id = tenantId:collection:externalId`. Champs : tenantId, collection, externalId, **title**, **content** (full-text), **source** (objet brut, `enabled=false` = stocké non indexé), indexedAt. |
| Repo | `repository/SearchDocRepository.java` | `ReactiveElasticsearchRepository`. |
| Mapping | `service/DocumentMapper.java` | Transforme l'objet JSON entrant en `SearchDoc` : extrait le `title` (name/title/label/code/sku/email…), aplatit récursivement tous les champs en `content`. |
| Indexation | `service/IndexService.java` | `index` / `indexBulk` / `delete`. |
| Recherche | `service/SearchService.java` | `CriteriaQuery` : filtre `tenantId` **obligatoire** + match `content`/`title`, filtre optionnel `collection`, pagination. |
| API | `api/IndexController.java` | `PUT /api/index/{collection}/{id}`, `POST .../_bulk`, `DELETE .../{id}`. `X-Tenant-Id` requis. |
| API | `api/SearchController.java` | `GET /api/search?q=&collection=&page=&size=`. `X-Tenant-Id` requis. |
| API | `api/ApiExceptionHandler.java` | 400 sur IllegalArgument/ServerWebInputException/binding. |
| Auth | `config/KernelAuthWebFilter.java` (+ Properties) | cf. §2. |

**Isolation tenant** : garantie dans `SearchService` (le filtre `tenantId` vient de l'en-tête
`X-Tenant-Id` et est toujours appliqué). Ne jamais retirer ce filtre.

---

## 5. Configuration (variables d'environnement)

| Variable | Défaut | Rôle |
|---|---|---|
| `SERVER_PORT` | `8080` | port HTTP |
| `ELASTICSEARCH_URIS` | `http://localhost:9200` | ES dédié (en prod `http://search-elasticsearch:9200`) |
| `SEARCH_AUTH_ENABLED` | `true` | mettre `false` **en DEV seulement** pour désactiver l'auth |
| `KERNEL_BASE_URL` | `http://kernel-core-kernel-layer-1:8080` | URL interne du kernel pour valider `/me` |
| `SEARCH_AUTH_CACHE_TTL` | `300` | durée (s) de cache d'un couple validé |

`.env.example` documente le set de prod. **Plus de `SEARCH_API_KEYS`.**

---

## 6. Build, CI/CD, déploiement

- **CI/CD** : GitHub Actions (repo `yowyob/yowyob-search`).
  - `.github/workflows/docker-publish.yml` : build → image GHCR `ghcr.io/yowyob/yowyob-search`
    (garde `github.repository == 'yowyob/yowyob-search'`, ignore `**.md`/`docs/**`).
  - `.github/workflows/deploy.yml` : sur `workflow_run` réussi → SSH sur le serveur, `pull` + `up -d`
    (s'appuie sur l'auth GHCR persistante du serveur ; `SEARCH_DIR=/home/gi/yowyob-search`).
- **Push sur `main`** = build + déploiement auto. (Les changements de docs seuls ne redéploient pas.)
- **Serveur** : `/home/gi/yowyob-search/` contient `docker-compose.prod.yml` + `.env`.
  `docker compose ps` pour l'état, `docker logs yowyob-search-yowyob-search-1` pour les logs.

> ⚠️ Les commits de ce dépôt n'ont **aucune attribution d'outil** (auteur `savhel`), c'est la règle.

---

## 7. Vérifier / déboguer en prod

```bash
# santé
curl -s https://search.yowyob.com/actuator/health        # → {"status":"UP"}

# auth : sans creds → 401 ; mauvaise clé → 401 ; vrai clientApplication kernel → 200
curl -s -o /dev/null -w '%{http_code}\n' "https://search.yowyob.com/api/search?q=x" \
  -H "X-Tenant-Id: 00000000-0000-0000-0000-000000000000"                                  # 401
curl -s "https://search.yowyob.com/api/search?q=x" \
  -H "X-Client-Id: <client>" -H "X-Api-Key: <secret>" -H "X-Tenant-Id: <tenant>"          # 200

# combien de docs au total (depuis le serveur, sur l'ES dédié) :
docker exec yowyob-search-search-elasticsearch-1 \
  curl -s "http://localhost:9200/yowyob-search-v1/_count"

# si 401 inattendu : vérifier que le conteneur joint le kernel
docker exec yowyob-search-yowyob-search-1 \
  wget -qO- http://kernel-core-kernel-layer-1:8080/api/client-applications/me \
  --header="X-Client-Id: <client>" --header="X-Api-Key: <secret>"
```

**Symptômes fréquents**
- *Tout en 401* → kernel injoignable depuis le conteneur (réseau `yowyob` ? `KERNEL_BASE_URL` ?) ou
  clientApplication révoqué côté kernel.
- *Un doc absent de la recherche mais présent dans `_count`* → tokenisation : voir §8.

---

## 8. Limites connues & pistes d'extension

- **Tokenisation / pertinence** : l'analyzer ES par défaut découpe certains champs de façon non
  intuitive (ex. une recherche `gmail` ne retrouve pas un email `x@gmail.com`). Pour des recherches
  partielles (sous-chaînes, emails, n-grams), définir un **mapping/analyzer custom** sur `title`/
  `content` (edge-ngram ou `search_as_you_type`). Point d'extension principal côté pertinence.
- **Cache d'auth** : `ConcurrentHashMap` simple (cf. §2). Passer à Caffeine si beaucoup de clés.
- **Ingestion** : push HTTP uniquement. Un connecteur Kafka d'auto-ingestion reste envisageable.
- **Réindexation full** : pas de job de backfill intégré ; les intégrateurs rejouent via `_bulk`.
- **Entitlements** : on n'applique que l'isolation tenant. On ne filtre pas par service/rôle du
  clientApplication (le `/me` du kernel renvoie pourtant ses `allowedServiceCodes` — exploitable si
  on veut restreindre les `collection` visibles par client).
