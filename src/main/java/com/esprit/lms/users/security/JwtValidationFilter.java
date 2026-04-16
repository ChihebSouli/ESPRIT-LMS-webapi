package com.esprit.lms.users.security;

import com.esprit.lms.users.entity.LmsRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

/**
 * JWT Validation Filter — runs on every request.
 *
 * Validates tokens issued by the ESPRIT auth microservice.
 * The LMS NEVER issues tokens — it only validates them.
 *
 * Auth MS JWT structure (HS256):
 *   - subject: username (email/identifier)
 *   - roles: List<String> (e.g., ["ETUDIANT"], ["ENSEIGNANT", "ADMIN"])
 *   - authorities: List<String> (Spring Security authorities)
 *   - student: String (étudiant ID, if role is ETUDIANT)
 *   - matricule: String (if role is ENSEIGNANT)
 *   - exp: expiration timestamp
 */
@Slf4j
@Component
public class JwtValidationFilter extends OncePerRequestFilter {

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();

            // Extract roles list from JWT
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            // Map to highest-privilege LMS role
            LmsRole lmsRole = RoleMapper.mapHighest(roles);

            // Extract optional claims
            String matricule = claims.get("matricule", String.class);
            Object studentClaim = claims.get("student");
            String studentId = studentClaim != null ? studentClaim.toString() : null;

            // Set authentication in security context
            JwtAuthentication auth = new JwtAuthentication(username, lmsRole, matricule, studentId);
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("JWT validated: user={}, role={}", username, lmsRole);

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"" + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT validation for public endpoints
        return path.equals("/api/v1/auth/sso-callback")
                || path.equals("/api/v1/purchases/webhook")
                || path.startsWith("/actuator");
    }
}
