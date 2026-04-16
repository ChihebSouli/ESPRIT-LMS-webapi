package com.esprit.lms.users.controller;

import com.esprit.lms.users.dto.UserProfileDTO;
import com.esprit.lms.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/v1/users/me — Current user profile from JWT context.
     * Auto-creates user record on first login.
     */
    @GetMapping("/me")
    public UserProfileDTO getCurrentUser() {
        return userService.getCurrentUserProfile();
    }
}
