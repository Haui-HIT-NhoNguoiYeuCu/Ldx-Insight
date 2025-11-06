package io.ldxinsight.service;

import io.ldxinsight.dto.AuthResponse;
import io.ldxinsight.dto.LoginRequest;
import io.ldxinsight.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}