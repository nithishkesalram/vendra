package com.procureai.auth.dto;

import com.procureai.auth.Role;
import java.util.Set;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        Long userId,
        String name,
        Set<Role> roles
) {
}
