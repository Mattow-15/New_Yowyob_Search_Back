package com.yowyob.search.infrastructure.adapters.out.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import java.util.List;

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

    @GeoPointField
    @Field(name = "location")
    private GeoPoint location;

    @Field(name = "latitude", type = FieldType.Double)
    private Double latitude;

    @Field(name = "longitude", type = FieldType.Double)
    private Double longitude;

    @Field(name = "images", type = FieldType.Keyword)
    private List<String> images;

    @Field(name = "imageUrl", type = FieldType.Keyword)
    private String imageUrl;

    @Field(name = "phone", type = FieldType.Keyword)
    private String phone;

    @Field(name = "website", type = FieldType.Keyword)
    private String website;

    @Field(name = "openingHours", type = FieldType.Text)
    private String openingHours;

    @Field(name = "reviewsCount", type = FieldType.Integer)
    private Integer reviewsCount;

    // ── Nouveaux champs Google Places ─────────────────────────────

    @Field(name = "openNow", type = FieldType.Boolean)
    private Boolean openNow;

    @Field(name = "priceLevel", type = FieldType.Integer)
    private Integer priceLevel;

    @Field(name = "reviewsSummary", type = FieldType.Text)
    private String reviewsSummary;

    @Field(name = "googleMapsUrl", type = FieldType.Keyword)
    private String googleMapsUrl;

    @Field(name = "street", type = FieldType.Keyword)
    private String street;

    @Field(name = "source", type = FieldType.Keyword)
    private String source;

    // ── Embedding sémantique ──────────────────────────────────────
    @Field(name = "embedding", type = FieldType.Dense_Vector, dims = 384)
    private float[] embedding;

    public GeoPoint getLocation() {
        if (location == null && latitude != null && longitude != null) {
            this.location = new GeoPoint(latitude, longitude);
        }
        return location;
    }
}