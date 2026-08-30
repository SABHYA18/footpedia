package com.footpedia.auth.dto;

import com.footpedia.domain.AppUser;

public record UserResponse(Long id, String email, String displayName) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
