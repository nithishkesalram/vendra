package com.procureai.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void generatedTokenCanBeValidatedAndParsed() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-with-at-least-thirty-two-characters",
                15,
                7
        );
        JwtService jwtService = new JwtService(properties);
        AppUser user = new AppUser();
        user.setId(42L);
        user.setEmail("admin@procureai.local");
        user.setFullName("Admin User");
        user.setRoles(Set.of(Role.ADMIN));

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.subject(token)).isEqualTo("admin@procureai.local");
    }
}
