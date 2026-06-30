package com.yowyob.auth.application.ports.out;

import com.yowyob.auth.domain.model.User;

public interface UserEventPublisherPort {
    void publishUserCreated(User user);
}
