package com.docsynth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OIDC/OAuth2 resource server configuration. Validates Bearer tokens against the
 * configured OIDC provider's JWKS endpoint. Tenant and user identity are derived
 * from the validated JWT claims; downstream filters enforce RBAC and RLS.
 *
 * Refs: FR-010 (OIDC/OAuth2 + RBAC), FR-012 (fail-closed on missing tenant).
 */
@Configuration
public class OidcConfig {

    private final OidcProperties properties;

    public OidcConfig(OidcProperties properties) {
        this.properties = properties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
            .withIssuerLocation(properties.getIssuerUri())
            .build();
    }
}
