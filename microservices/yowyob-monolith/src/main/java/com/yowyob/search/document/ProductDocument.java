package com.yowyob.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * Product Document for Elasticsearch.
 * Uses 'title' and 'serviceType' consistently.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "crawler-index")
public class ProductDocument {
    @Id
    private String id;

    @Field(name = "title", type = FieldType.Text)
    private String title;

    @Field(name = "description", type = FieldType.Text)
    private String description;

    @Field(name = "price", type = FieldType.Double)
    private Double price;

    @Field(name = "serviceType", type = FieldType.Keyword)
    private String serviceType;

    @Field(name = "type", type = FieldType.Keyword)
    private String type;

    @Field(name = "category", type = FieldType.Keyword)
    private String category;

    @Field(name = "city", type = FieldType.Keyword)
    private String city;

    @Field(name = "quartier", type = FieldType.Keyword)
    private String quartier;

    @Field(name = "rating", type = FieldType.Double)
    private Double rating;

    @Field(name = "location")
    private String location;

    @Field(name = "latitude", type = FieldType.Double)
    private Double latitude;

    @Field(name = "longitude", type = FieldType.Double)
    private Double longitude;

    @Field(name = "images", type = FieldType.Keyword)
    private List<String> images;

    @Field(name = "text_vector", type = FieldType.Dense_Vector, dims = 384, index = true)
    private float[] textVector;
}