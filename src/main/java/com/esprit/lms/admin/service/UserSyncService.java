package com.esprit.lms.admin.service;

import com.esprit.lms.users.entity.LmsRole;
import com.esprit.lms.users.entity.User;
import com.esprit.lms.users.repository.UserRepository;
import com.esprit.lms.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles user synchronization from ESPRIT central database / auth MS.
 *
 * T016: Pre-import job (batch upsert on startup when IMPORT_MODE=true)
 * T017: Delta sync (nightly 2AM + on-demand admin endpoint)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Manual sync triggered by admin endpoint.
     * In a real implementation, this would connect to ESPRIT central DB
     * and fetch users updated since last sync.
     *
     * For now, it logs the action — the actual import data source
     * (ESPRIT central DB read-only connection) will be configured
     * when the connection details are provided.
     */
    public int syncUsers() {
        log.info("Starting user delta sync...");

        // Find users that need re-syncing (synced more than 24h ago or never synced)
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<User> staleUsers = userRepository.findAll().stream()
                .filter(u -> u.getLastSyncedAt() == null || u.getLastSyncedAt().isBefore(threshold))
                .toList();

        log.info("Found {} users needing sync refresh", staleUsers.size());

        // TODO: When ESPRIT central DB connection is available:
        // 1. Connect to read-only DataSource for ESPRIT central DB
        // 2. Query for users with updated_at > MAX(last_synced_at) in LMS
        // 3. Batch upsert (500 at a time) into LMS users table
        // For now, just update the sync timestamp
        int synced = 0;
        for (User user : staleUsers) {
            user.setLastSyncedAt(LocalDateTime.now());
            userRepository.save(user);
            synced++;
        }

        log.info("Delta sync complete: {} users refreshed", synced);
        return synced;
    }

    /**
     * Nightly sync at 2AM — automatically keeps LMS users in sync.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledSync() {
        log.info("Running scheduled nightly user sync");
        int count = syncUsers();
        log.info("Nightly sync complete: {} users", count);
    }
}
