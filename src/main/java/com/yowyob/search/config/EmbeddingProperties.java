package com.yowyob.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de la recherche sémantique (embeddings vectoriels).
 *
 * <p>Quand {@code enabled=true}, chaque document indexé est enrichi d'un vecteur
 * {@code text_vector} (via le micro-service {@code yowyob-embeddings}) et les requêtes
 * combinent le lexical (edge-ngram) et le kNN sémantique. Quand {@code enabled=false}
 * — ou si le service d'embeddings est injoignable — le service dégrade vers le lexical seul.
 *
 * @param enabled        active la génération/recherche vectorielle.
 * @param url            base URL du micro-service yowyob-embeddings.
 * @param dimensions     dimension du vecteur, doit coller au modèle et au mapping ES (384).
 * @param k              nombre de voisins kNN à retourner.
 * @param numCandidates  nombre de candidats explorés par shard (qualité/coût du kNN).
 * @param boost          poids du score sémantique relatif au lexical.
 */
@ConfigurationProperties(prefix = "yowyob.search.embedding")
public record EmbeddingProperties(
        boolean enabled,
        String url,
        int dimensions,
        int k,
        int numCandidates,
        float boost) {

    public EmbeddingProperties {
        if (url == null || url.isBlank()) {
            url = "http://localhost:8000";
        }
        if (dimensions <= 0) {
            dimensions = 384;
        }
        if (k <= 0) {
            k = 20;
        }
        if (numCandidates <= 0) {
            numCandidates = 100;
        }
        if (boost <= 0) {
            boost = 2.0f;
        }
    }
}
