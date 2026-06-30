package com.yowyob.auth.infrastructure.adapters.in.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
