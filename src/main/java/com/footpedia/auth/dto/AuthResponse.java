package com.footpedia.auth.dto;

public record AuthResponse(String token, String email, String displayName) {
}
