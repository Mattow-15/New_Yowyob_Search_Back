package com.yowyob.notification.controller;

import com.yowyob.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for managing and testing notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/test-email")
    @Operation(summary = "Test Email Sending", description = "Manually triggers a simulated email notification")
    public ResponseEntity<String> testEmail(@RequestParam String to, @RequestParam String subject,
            @RequestParam String body) {
        notificationService.sendNotification(to, subject, body);
        return ResponseEntity.ok("Notification simulation triggered. Check logs.");
    }

    @GetMapping("/health")
    public String health() {
        return "Notification Service is running!";
    }
}
