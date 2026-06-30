package com.yowyob.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

/**
 * Document Elasticsearch représentant un historique de recherche utilisateur.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "search_history")
public class SearchHistory {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Text)
    private String query;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Date)
    private Instant timestamp;
}
