package com.esprit.lms.admin.controller;

import com.esprit.lms.admin.service.SystemConfigService;
import com.esprit.lms.admin.service.UserSyncService;
import com.esprit.lms.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserSyncService userSyncService;
    private final UserService userService;
    private final SystemConfigService configService;

    /**
     * POST /api/v1/admin/users/sync — Trigger user delta sync.
     */
    @PostMapping("/users/sync")
    public Map<String, Object> syncUsers() {
        int count = userSyncService.syncUsers();
        return Map.of("synced", count, "status", "complete");
    }

    /**
     * PUT /api/v1/admin/users/{id}/borrow-limit — Set borrow limit for a user.
     */
    @PutMapping("/users/{id}/borrow-limit")
    public Map<String, String> setBorrowLimit(@PathVariable UUID id,
                                               @RequestParam int limit) {
        userService.setBorrowLimit(id, limit);
        return Map.of("status", "updated");
    }

    /**
     * GET /api/v1/admin/config/{key} — Get a system config value.
     */
    @GetMapping("/config/{key}")
    public Map<String, String> getConfig(@PathVariable String key) {
        String value = configService.getValue(key);
        return Map.of("key", key, "value", value != null ? value : "");
    }

    /**
     * PUT /api/v1/admin/config/{key} — Update a system config value.
     */
    @PutMapping("/config/{key}")
    public Map<String, String> setConfig(@PathVariable String key,
                                          @RequestParam String value) {
        configService.setValue(key, value);
        return Map.of("key", key, "value", value, "status", "updated");
    }

    // TODO Sprint 11: GET /api/v1/admin/analytics
    // TODO Sprint 10: GET/PUT /api/v1/admin/email-templates
}
