package com.footpedia.auth;

import com.footpedia.auth.dto.AuthResponse;
import com.footpedia.auth.dto.LoginRequest;
import com.footpedia.auth.dto.SignupRequest;
import com.footpedia.domain.AppUser;
import com.footpedia.repository.AppUserRepository;
import com.footpedia.web.exception.ConflictException;
import com.footpedia.web.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse signup(SignupRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setCreatedAt(Instant.now());
        user = appUserRepository.save(user);

        return new AuthResponse(jwtService.issueToken(user.getId(), user.getEmail()), user.getEmail(), user.getDisplayName());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository
                .findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        return new AuthResponse(jwtService.issueToken(user.getId(), user.getEmail()), user.getEmail(), user.getDisplayName());
    }
}
