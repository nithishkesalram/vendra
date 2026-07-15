package com.procureai.auth;

import com.procureai.auth.dto.LoginRequest;
import com.procureai.auth.dto.LoginResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenHasher tokenHasher;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            RefreshTokenHasher tokenHasher
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        String hash = tokenHasher.hash(refreshToken);
        AppUser user = userRepository.findByRefreshTokenHash(hash)
                .filter(AppUser::isActive)
                .filter(candidate -> candidate.getRefreshTokenExpiresAt() != null)
                .filter(candidate -> candidate.getRefreshTokenExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        return issueTokens(user);
    }

    private LoginResponse issueTokens(AppUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = randomToken();
        user.setRefreshTokenHash(tokenHasher.hash(refreshToken));
        user.setRefreshTokenExpiresAt(Instant.now().plus(jwtProperties.refreshTokenDays(), ChronoUnit.DAYS));
        userRepository.save(user);
        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenMinutes() * 60,
                user.getId(),
                user.getFullName(),
                user.getRoles()
        );
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
