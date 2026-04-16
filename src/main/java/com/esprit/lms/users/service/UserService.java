package com.esprit.lms.users.service;

import com.esprit.lms.shared.exception.ApiException;
import com.esprit.lms.users.dto.UserProfileDTO;
import com.esprit.lms.users.entity.LmsRole;
import com.esprit.lms.users.entity.User;
import com.esprit.lms.users.repository.UserRepository;
import com.esprit.lms.users.security.JwtAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get or auto-create a user from the current JWT context.
     * On first login, creates a user record from JWT claims.
     */
    @Transactional
    public User getOrCreateCurrentUser() {
        JwtAuthentication auth = getCurrentAuth();
        String username = auth.getUsername();

        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByEspritId(username))
                .orElseGet(() -> {
                    log.info("First login — creating user: {}", username);
                    User newUser = User.builder()
                            .espritId(username)
                            .email(username)
                            .fullName(username)  // Will be enriched by sync job
                            .role(auth.getLmsRole())
                            .matricule(auth.getMatricule())
                            .isActive(true)
                            .lastSyncedAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    public UserProfileDTO getCurrentUserProfile() {
        User user = getOrCreateCurrentUser();
        int effectiveLimit = user.getBorrowLimit() != null ? user.getBorrowLimit() : 3;

        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .matricule(user.getMatricule())
                .effectiveBorrowLimit(effectiveLimit)
                .build();
    }

    @Transactional
    public void setBorrowLimit(UUID userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        user.setBorrowLimit(limit);
        userRepository.save(user);
        log.info("Borrow limit for {} set to {}", user.getEmail(), limit);
    }

    /**
     * Upsert a user from external data (ESPRIT central DB or auth MS).
     */
    @Transactional
    public User upsertUser(String espritId, String email, String fullName,
                           LmsRole role, String matricule) {
        User user = userRepository.findByEspritId(espritId)
                .orElse(User.builder()
                        .espritId(espritId)
                        .email(email)
                        .build());

        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setMatricule(matricule);
        user.setIsActive(true);
        user.setLastSyncedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public JwtAuthentication getCurrentAuth() {
        return (JwtAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }
}
