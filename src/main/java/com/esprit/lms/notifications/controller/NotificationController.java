package com.esprit.lms.notifications.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("module", "notifications", "status", "ready");
    }

    // Notification module is primarily backend-only (no user-facing endpoints).
    // Email sending is triggered by other modules via NotificationService.
    // TODO Sprint 10: Build EmailProvider, NotificationService, wire events
}
