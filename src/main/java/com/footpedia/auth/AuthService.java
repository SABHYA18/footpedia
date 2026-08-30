package com.footpedia.auth;

import com.footpedia.auth.dto.AuthResponse;
import com.footpedia.auth.dto.LoginRequest;
import com.footpedia.auth.dto.SignupRequest;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);
}
