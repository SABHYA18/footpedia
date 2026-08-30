package com.footpedia.auth;

import com.footpedia.auth.dto.AuthResponse;
import com.footpedia.auth.dto.LoginRequest;
import com.footpedia.auth.dto.MessageResponse;
import com.footpedia.auth.dto.PasswordResetConfirmRequest;
import com.footpedia.auth.dto.PasswordResetRequest;
import com.footpedia.auth.dto.SignupRequest;
import com.footpedia.auth.dto.UserResponse;
import com.footpedia.domain.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUser user) {
        return UserResponse.from(user);
    }

    @PostMapping("/password-reset/request")
    public MessageResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request);
        return new MessageResponse("If that email is registered, a reset link has been sent.");
    }

    @PostMapping("/password-reset/confirm")
    public MessageResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);
        return new MessageResponse("Password updated. You can now log in with your new password.");
    }
}
