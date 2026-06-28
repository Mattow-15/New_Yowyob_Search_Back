package com.yowyob.search.domain;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * Document générique indexé par le service. N'importe quel projet pousse un objet JSON arbitraire
 * dans une {@code collection} (= type logique : "products", "invoices", "users"…), scopé par
 * {@code tenantId}.
 *
 * <p>{@code id} = {@code tenantId:collection:externalId} → upsert idempotent, pas de collision
 * inter-tenant/collection. {@code content} = aplatissement texte de la source (full-text).
 * {@code source} = objet brut renvoyé tel quel dans les résultats (non indexé pour éviter
 * l'explosion de mapping entre collections hétérogènes).
 *
 * <p>{@code title}/{@code content} sont indexés avec un analyzer <b>edge-ngram</b> (préfixes, 2→20)
 * et interrogés avec l'analyzer {@code standard} : une saisie partielle ({@code "gmail"} dans
 * {@code "x@gmail.com"}, {@code "arab"} dans {@code "Arabica"}) matche, façon « search as you type ».
 *
 * <p>{@code textVector} ({@code dense_vector}, 384 dims, similarité cosinus) porte l'embedding
 * sémantique du document, généré par {@code yowyob-embeddings}. Il alimente la branche kNN de la
 * recherche hybride. Le champ est optionnel : un document sans vecteur reste trouvable en lexical.
 *
 * <p>Le mapping (analyzer + dense_vector) ne pouvant pas changer sur un index existant, l'index est
 * versionné. L'ajout du vecteur fait passer de {@code -v2} à {@code -v3} : un index neuf est créé,
 * les données doivent être ré-indexées (re-push des clients) pour bénéficier du sémantique.
 */
@Document(indexName = "yowyob-search-v3", createIndex = true)
@Setting(settingPath = "elasticsearch/settings.json")
public record SearchDoc(
        @Id String id,
        @Field(type = FieldType.Keyword) String tenantId,
        @Field(type = FieldType.Keyword) String collection,
        @Field(type = FieldType.Keyword) String externalId,
        @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "standard") String title,
        @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "standard") String content,
        @Field(type = FieldType.Object, enabled = false) Map<String, Object> source,
        @Field(type = FieldType.Dense_Vector, dims = 384, index = true, similarity = "cosine") float[] textVector,
        @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX") Instant indexedAt) {

    public static String documentId(String tenantId, String collection, String externalId) {
        return tenantId + ":" + collection + ":" + externalId;
    }

    /** Copie du document avec son vecteur sémantique renseigné. */
    public SearchDoc withTextVector(float[] vector) {
        return new SearchDoc(id, tenantId, collection, externalId, title, content, source, vector, indexedAt);
    }
}
