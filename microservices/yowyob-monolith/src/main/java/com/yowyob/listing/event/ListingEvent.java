package com.yowyob.listing.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Événement RabbitMQ émis lors de la création, modification ou suppression
 * d'une annonce.
 * Consommé par les modules search et notification pour la synchronisation.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListingEvent implements Serializable {
    private UUID id;
    private String title;
    private String description;
    private Double price;
    private String category;
    private String address;
    private String street;
    private Double latitude;
    private Double longitude;
    private String status;
    private UUID sellerId;
    private String eventType; // CREATED, UPDATED, DELETED
}
