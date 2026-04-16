package com.esprit.lms.users.security;

import com.esprit.lms.users.entity.LmsRole;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Custom authentication token stored in SecurityContextHolder after JWT validation.
 * Gives controllers access to the current user's identity and role.
 */
@Getter
public class JwtAuthentication extends AbstractAuthenticationToken {

    private final String username;   // from JWT subject (the auth MS username/email)
    private final LmsRole lmsRole;   // mapped from JWT roles claim
    private final String matricule;  // from JWT claims (if present)
    private final String studentId;  // from JWT "student" claim (if étudiant)

    public JwtAuthentication(String username, LmsRole lmsRole, String matricule, String studentId) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + lmsRole.name())));
        this.username = username;
        this.lmsRole = lmsRole;
        this.matricule = matricule;
        this.studentId = studentId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // JWT is stateless — no credentials stored
    }

    @Override
    public Object getPrincipal() {
        return username;
    }
}
