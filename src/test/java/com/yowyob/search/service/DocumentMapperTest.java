package com.yowyob.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yowyob.search.domain.SearchDoc;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DocumentMapperTest {

    @Test
    void buildsDocumentWithCompositeIdTitleAndFlattenedContent() {
        SearchDoc doc = DocumentMapper.toDocument("tenant-1", "products", "p-42",
                Map.of("name", "Café Arabica", "sku", "CAF-001", "price", 1500));

        assertThat(doc.id()).isEqualTo("tenant-1:products:p-42");
        assertThat(doc.tenantId()).isEqualTo("tenant-1");
        assertThat(doc.collection()).isEqualTo("products");
        assertThat(doc.externalId()).isEqualTo("p-42");
        assertThat(doc.title()).isEqualTo("Café Arabica");
        assertThat(doc.content()).contains("Café Arabica").contains("CAF-001").contains("1500");
        assertThat(doc.source()).containsEntry("sku", "CAF-001");
        assertThat(doc.indexedAt()).isNotNull();
    }

    @Test
    void flattensNestedMapsAndCollections() {
        String flat = DocumentMapper.flatten(Map.of(
                "owner", Map.of("firstName", "Jordan", "lastName", "Toulépi"),
                "tags", List.of("vip", "actif")));

        assertThat(flat).contains("Jordan").contains("Toulépi").contains("vip").contains("actif");
    }

    @Test
    void titleIsNullWhenNoRecognizableKey() {
        SearchDoc doc = DocumentMapper.toDocument("t", "misc", "x",
                Map.of("randomField", "value", "amount", 10));
        assertThat(doc.title()).isNull();
        assertThat(doc.content()).contains("value").contains("10");
    }
}
