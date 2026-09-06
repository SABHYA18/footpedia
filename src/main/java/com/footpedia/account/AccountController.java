package com.footpedia.account;

import com.footpedia.account.dto.ChangePasswordRequest;
import com.footpedia.account.dto.DeleteAccountRequest;
import com.footpedia.account.dto.UpdateProfileRequest;
import com.footpedia.auth.dto.MessageResponse;
import com.footpedia.auth.dto.UserResponse;
import com.footpedia.domain.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PutMapping("/profile")
    public UserResponse updateProfile(@AuthenticationPrincipal AppUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return accountService.updateProfile(user.getId(), request);
    }

    @PostMapping("/password/code")
    public MessageResponse requestPasswordChangeCode(@AuthenticationPrincipal AppUser user) {
        accountService.requestPasswordChangeCode(user.getId());
        return new MessageResponse("Verification code sent to your email.");
    }

    @PutMapping("/password")
    public MessageResponse changePassword(@AuthenticationPrincipal AppUser user, @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(user.getId(), request);
        return new MessageResponse("Password updated.");
    }

    @DeleteMapping
    public MessageResponse deleteAccount(@AuthenticationPrincipal AppUser user, @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(user.getId(), request);
        return new MessageResponse("Account deleted.");
    }
}
