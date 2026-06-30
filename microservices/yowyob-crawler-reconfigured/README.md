# YowYob Crawler - Version Simplifiée

Service de crawling simplifié pour tester l'extraction de données de sites web.

## 🚀 Démarrage rapide

### 1. Compiler le projet

```bash
cd yowyob-crawler-reconfigured
mvn clean package -DskipTests
```

### 2. Lancer l'application

```bash
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8086**

## 📡 Endpoints disponibles

### Health Check
```bash
GET http://localhost:8086/api/crawler/health
```

### Test simple
```bash
GET http://localhost:8086/api/crawler/test
```

### Crawler une URL
```bash
POST http://localhost:8086/api/crawler/crawl?url=<URL>
```

**Exemple:**
```bash
curl -X POST "http://localhost:8086/api/crawler/crawl?url=http://localhost:3000/"
```

### Statistiques
```bash
GET http://localhost:8086/api/crawler/stats
```

## 🧪 Tester avec le backend de test

### 1. Lancer le backend de test (port 3000)

Dans un terminal séparé:
```bash
cd test-backend-springboot
mvn spring-boot:run
```

### 2. Lancer le crawler (port 8086)

Dans un autre terminal:
```bash
cd yowyob-crawler-reconfigured
mvn spring-boot:run
```

### 3. Tester le crawling

```bash
# Page d'accueil
curl -X POST "http://localhost:8086/api/crawler/crawl?url=http://localhost:3000/"

# Page produit
curl -X POST "http://localhost:8086/api/crawler/crawl?url=http://localhost:3000/product/1"

# Catégorie
curl -X POST "http://localhost:8086/api/crawler/crawl?url=http://localhost:3000/category/electronics"

# API JSON
curl -X POST "http://localhost:8086/api/crawler/crawl?url=http://localhost:3000/api/products"
```

## 📊 Format de réponse

```json
{
  "url": "http://localhost:3000/",
  "success": true,
  "errorMessage": null,
  "httpStatusCode": 200,
  "crawledAt": "2026-01-20T10:30:00",
  "durationMs": 1234,
  "extractedData": {
    "title": "Boutique Test E-commerce",
    "description": "Site de test pour crawler YowYob",
    "products": [
      {
        "name": "Téléphone Samsung Galaxy",
        "price": "250000 FCFA",
        "description": "Smartphone dernière génération"
      }
    ],
    "links": ["http://localhost:3000/product/1", ...],
    "headings": {
      "h1": ["Boutique Test"],
      "h2": ["Nos Produits"]
    }
  },
  "itemsFound": 15,
  "discoveredLinks": [...],
  "pageSize": 5678,
  "detectedLanguage": "fr",
  "extractorUsed": "GenericExtractor"
}
```

## 🔧 Configuration

Modifiez `src/main/resources/application.yml` pour:
- Changer le port (par défaut: 8086)
- Ajuster le timeout
- Configurer le logging

## 📝 Ajouter un extracteur personnalisé

Pour créer un extracteur pour un site spécifique:

1. Créez une classe qui implémente `SiteExtractor`
2. Annotez-la avec `@Component`
3. Implémentez les méthodes:
   - `canHandle(String url)`: retourne true si l'extracteur peut gérer cette URL
   - `getPriority()`: retourne un entier (plus petit = plus prioritaire)
   - `extract(Document document, String url)`: extrait les données

**Exemple:**

```java
@Component
public class MyCustomExtractor implements SiteExtractor {
    
    @Override
    public boolean canHandle(String url) {
        return url.contains("mysite.com");
    }
    
    @Override
    public int getPriority() {
        return 10; // Plus prioritaire que GenericExtractor (Integer.MAX_VALUE)
    }
    
    @Override
    public Map<String, Object> extract(Document document, String url) {
        Map<String, Object> data = new HashMap<>();
        // Votre logique d'extraction ici
        return data;
    }
}
```

## 🐛 Dépannage

### Le service ne démarre pas
- Vérifiez que le port 8086 est libre: `sudo lsof -i :8086`
- Vérifiez les logs dans la console

### Erreur de connexion lors du crawl
- Vérifiez que l'URL cible est accessible
- Vérifiez que le backend de test tourne (si vous testez localement)

### Timeout
- Augmentez le timeout dans `application.yml`
- L'URL peut être lente ou inaccessible

## 📦 Structure du projet

```
yowyob-crawler-reconfigured/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/yowyob/crawler/
│       │   ├── CrawlerApplication.java
│       │   ├── controller/
│       │   │   └── CrawlerController.java
│       │   ├── service/
│       │   │   └── CrawlerService.java
│       │   ├── model/
│       │   │   └── CrawlResult.java
│       │   └── extractor/
│       │       ├── SiteExtractor.java
│       │       └── GenericExtractor.java
│       └── resources/
│           └── application.yml
└── README.md
```

## ✨ Fonctionnalités

- ✅ Extraction automatique de métadonnées (title, description, keywords)
- ✅ Extraction de produits e-commerce
- ✅ Extraction de liens et images
- ✅ Support de n'importe quel site web (GenericExtractor)
- ✅ Extensible via extracteurs personnalisés
- ✅ Logging détaillé
- ✅ Monitoring via Spring Actuator
- ✅ Aucune dépendance externe (pas de Redis, RabbitMQ, etc.)
