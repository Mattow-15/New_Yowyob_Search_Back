package com.yowyob.notification.infrastructure.adapters.in.web;

import com.yowyob.notification.application.ports.in.SendNotificationUseCase;
import com.yowyob.notification.domain.model.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for managing and testing notifications")
public class NotificationController {

    private final SendNotificationUseCase sendNotificationUseCase;

    @PostMapping("/test-email")
    @Operation(summary = "Test Email Sending", description = "Manually triggers a simulated email notification")
    public ResponseEntity<String> testEmail(@RequestParam String to, @RequestParam String subject,
            @RequestParam String body) {
        Notification notification = Notification.builder()
                .recipient(to)
                .subject(subject)
                .message(body)
                .build();
        sendNotificationUseCase.sendNotification(notification);
        return ResponseEntity.ok("Notification simulation triggered. Check logs.");
    }

    @GetMapping("/health")
    public String health() {
        return "Notification Service is running!";
    }
}
