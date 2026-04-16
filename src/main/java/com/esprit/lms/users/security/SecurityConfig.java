package com.esprit.lms.users.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtValidationFilter jwtValidationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/sso-callback").permitAll()
                .requestMatchers("/api/v1/purchases/webhook").permitAll()
                .requestMatchers("/actuator/**").permitAll()

                // Catalog write operations → LIBRARIAN or ADMIN
                .requestMatchers(HttpMethod.POST, "/api/v1/catalog").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/catalog/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/catalog/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/catalog/*/reindex").hasAnyRole("LIBRARIAN", "ADMIN")

                // Loan management → LIBRARIAN or ADMIN
                .requestMatchers(HttpMethod.POST, "/api/v1/loans/*/checkout").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/loans/*/return").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/loans/*/extend").hasAnyRole("LIBRARIAN", "ADMIN")

                // Desk payments → LIBRARIAN
                .requestMatchers(HttpMethod.PUT, "/api/v1/purchases/desk/*/pay").hasAnyRole("LIBRARIAN", "ADMIN")

                // Admin endpoints → ADMIN only
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // All other /api/** require authentication
                .requestMatchers("/api/**").authenticated()

                // Everything else (static resources, etc.)
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtValidationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
