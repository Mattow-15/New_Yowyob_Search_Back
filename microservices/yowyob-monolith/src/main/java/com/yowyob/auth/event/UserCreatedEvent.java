package com.yowyob.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement émis lors de la création d'un nouvel utilisateur.
 * Utilisé pour la synchronisation inter-modules via RabbitMQ.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {
    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
}
