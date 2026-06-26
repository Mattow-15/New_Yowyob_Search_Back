# Guide développeur — yowyob-search

Comment intégrer le service de recherche **yowyob-search** dans votre projet.
Service autonome, multi-tenant, accessible par HTTP depuis n'importe quel langage.

- **Base URL (prod)** : `https://search.yowyob.com`
- **Santé** : `GET /actuator/health` (ouvert, sans clé)

---

## 1. Quand utiliser yowyob-search ? (vs la recherche interne du kernel)

Il existe **deux** recherches sur la plateforme. Choisissez selon votre projet :

| | **yowyob-search** (ce service) | **Recherche interne du kernel** (`/api/search` du kernel) |
|---|---|---|
| Pour qui | **N'importe quel projet** (kernel ou non : fleet, rental, apps Python/Node…) | Uniquement les projets bâtis **sur le kernel** |
| Données indexées | Ce **que vous poussez** vous-même (push HTTP) | Les entités du kernel, **automatiquement** (via ses événements) |
| Vous gérez l'indexation ? | **Oui** : vous indexez à chaque create/update/delete | **Non** : le kernel s'en charge |
| Isolation | Multi-tenant (`X-Tenant-Id`) | Multi-tenant (contexte d'auth kernel) |
| Cas d'usage | Recherche propre à votre produit, données hors kernel, contrôle total du contenu indexé | Recherche transverse sur les données déjà dans le kernel |

> **Règle simple** : vos données sont **dans le kernel** → utilisez la recherche du kernel. Vos
> données sont **ailleurs** (ou vous voulez maîtriser ce qui est indexé) → utilisez **yowyob-search**.

---

## 2. Prérequis

Demandez à l'équipe plateforme :
- une **clé d'API** (`X-Api-Key`) propre à votre projet ;
- l'identifiant **tenant** (`X-Tenant-Id`) de vos données (souvent l'organisation/le client).

> 🔒 **Ces appels se font côté SERVEUR uniquement.** Ne mettez jamais la clé d'API dans le
> navigateur / une app mobile / un bundle front. Votre backend appelle yowyob-search ; votre front
> appelle votre backend.

Tous les endpoints `/api/**` exigent les deux en-têtes :
```
X-Api-Key: <votre clé>
X-Tenant-Id: <votre tenant>
```

---

## 3. Concepts

- **collection** : un type logique, librement choisi (`products`, `invoices`, `clients`, `vehicles`…).
  C'est juste un libellé qui regroupe des documents de même nature.
- **id** : l'identifiant **de votre côté** (la PK de votre entité). Réindexer le même id = mise à jour.
- **document** : un objet JSON quelconque. Le service en extrait automatiquement :
  - un **title** (depuis `name`/`title`/`label`/`code`/`sku`/`email`… s'il existe),
  - un **content** plein texte (aplatissement récursif de tous les champs) pour la recherche.
  - la **source** brute est conservée et renvoyée telle quelle dans les résultats.

---

## 4. Indexer des documents

### Upsert d'un document
`PUT /api/index/{collection}/{id}` — crée ou remplace.
```bash
curl -X PUT https://search.yowyob.com/api/index/products/p-42 \
  -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{"name":"Café Arabica","sku":"CAF-001","price":1500,"tags":["bio","local"]}'
# → 200 {"ref":"<tenant>:products:p-42","indexed":1}
```

### Indexation en lot
`POST /api/index/{collection}/_bulk` — chaque élément **doit** porter un champ `id`.
```bash
curl -X POST https://search.yowyob.com/api/index/products/_bulk \
  -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT" -H "Content-Type: application/json" \
  -d '[{"id":"p-1","name":"Article A"},{"id":"p-2","name":"Article B"}]'
# → 200 {"ref":"products","indexed":2}
```

### Suppression
`DELETE /api/index/{collection}/{id}`
```bash
curl -X DELETE https://search.yowyob.com/api/index/products/p-42 \
  -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT"
# → 204
```

---

## 5. Rechercher

`GET /api/search?q=<texte>&collection=<optionnel>&page=0&size=20`
```bash
curl "https://search.yowyob.com/api/search?q=arabica&collection=products" \
  -H "X-Api-Key: $KEY" -H "X-Tenant-Id: $TENANT"
```
```json
{
  "count": 1,
  "results": [
    { "collection": "products", "id": "p-42", "title": "Café Arabica",
      "source": { "name": "Café Arabica", "sku": "CAF-001", "price": 1500, "tags": ["bio","local"] } }
  ]
}
```
- `q` : obligatoire (recherche sur `title` + tous les champs via `content`).
- `collection` : optionnel (omis = tous les types du tenant).
- `page` / `size` : pagination (`size` max 100, défaut 20).
- Résultats **toujours** limités à votre `X-Tenant-Id`.

---

## 6. Exemples d'intégration

### Node.js / TypeScript (backend)
```ts
const BASE = "https://search.yowyob.com";
const headers = {
  "X-Api-Key": process.env.SEARCH_API_KEY!,
  "X-Tenant-Id": tenantId,
  "Content-Type": "application/json",
};

// indexer à la création / mise à jour d'un produit
await fetch(`${BASE}/api/index/products/${product.id}`, {
  method: "PUT", headers, body: JSON.stringify(product),
});

// supprimer
await fetch(`${BASE}/api/index/products/${id}`, { method: "DELETE", headers });

// rechercher (depuis votre route /search)
const r = await fetch(`${BASE}/api/search?q=${encodeURIComponent(q)}&collection=products`, { headers });
const { results } = await r.json();
```

### Python (backend)
```python
import os, requests
BASE = "https://search.yowyob.com"
H = {"X-Api-Key": os.environ["SEARCH_API_KEY"], "X-Tenant-Id": tenant_id}

requests.put(f"{BASE}/api/index/clients/{c['id']}", headers=H, json=c)          # upsert
requests.delete(f"{BASE}/api/index/clients/{client_id}", headers=H)             # delete
r = requests.get(f"{BASE}/api/search", headers=H, params={"q": q, "collection": "clients"})
results = r.json()["results"]
```

---

## 7. Bonnes pratiques

1. **Gardez l'index synchrone** avec votre base : indexez à chaque **create/update**, supprimez à
   chaque **delete**. L'`id` = votre PK → l'upsert est idempotent (rejouable sans doublon).
2. **Réindexation initiale** : pour brancher un projet existant, parcourez vos entités une fois et
   poussez-les via `_bulk` (par lots de quelques centaines).
3. **Que mettre dans le document ?** Les champs sur lesquels on doit pouvoir chercher **et** ceux à
   afficher dans les résultats (la `source` est renvoyée telle quelle). Évitez les secrets/données
   sensibles inutiles — c'est un index de recherche.
4. **Une `collection` par type d'entité** : facilite le filtrage et l'affichage.
5. **Tolérance aux pannes** : l'indexation est annexe. Si un appel échoue, journalisez et
   continuez (ou rejouez en tâche de fond) — ne bloquez pas votre opération métier dessus.
6. **Sécurité** : clé d'API et appels **server-side only**. Le navigateur ne parle qu'à votre backend.

---

## 8. Codes de réponse

| Code | Sens |
|---|---|
| 200 | OK (index / recherche) |
| 204 | Supprimé |
| 400 | Requête invalide (en-tête `X-Tenant-Id` manquant, `q` absent, lot sans `id`…) |
| 401 | `X-Api-Key` absente ou invalide |

---

## 9. Limites actuelles / à venir

- Ingestion **par push HTTP** uniquement (un connecteur Kafka d'auto-ingestion est envisagé).
- Recherche full-text + filtre par `collection` ; le scoring/“fuzzy”/facettes avancées viendront.
- Pas encore d'API d'administration des clés (gérées dans le `.env` serveur pour l'instant).
