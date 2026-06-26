package com.yowyob.search.domain;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Document générique indexé par le service. N'importe quel projet pousse un objet JSON arbitraire
 * dans une {@code collection} (= type logique : "products", "invoices", "users"…), scopé par
 * {@code tenantId}.
 *
 * <p>{@code id} = {@code tenantId:collection:externalId} → upsert idempotent, pas de collision
 * inter-tenant/collection. {@code content} = aplatissement texte de la source (full-text).
 * {@code source} = objet brut renvoyé tel quel dans les résultats (non indexé pour éviter
 * l'explosion de mapping entre collections hétérogènes).
 */
@Document(indexName = "yowyob-search-v1", createIndex = true)
public record SearchDoc(
        @Id String id,
        @Field(type = FieldType.Keyword) String tenantId,
        @Field(type = FieldType.Keyword) String collection,
        @Field(type = FieldType.Keyword) String externalId,
        @Field(type = FieldType.Text) String title,
        @Field(type = FieldType.Text) String content,
        @Field(type = FieldType.Object, enabled = false) Map<String, Object> source,
        @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX") Instant indexedAt) {

    public static String documentId(String tenantId, String collection, String externalId) {
        return tenantId + ":" + collection + ":" + externalId;
    }
}
