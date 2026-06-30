package com.yowyob.notification.infrastructure.adapters.in.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListingEvent implements Serializable {
    private UUID id;
    private String title;
    private String description;
    private Double price;
    private String category;
    private String street;
    private UUID sellerId;
    private String eventType;
}
