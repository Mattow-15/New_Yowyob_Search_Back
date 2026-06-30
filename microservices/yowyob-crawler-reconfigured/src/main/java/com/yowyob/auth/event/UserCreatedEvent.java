package com.yowyob.auth.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {

    private String id;
    private String email;
    private String username;   // si tu as
    private String firstName;  // <-- ajouté
    private String lastName;   // <-- ajouté
}
