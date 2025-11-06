package io.ldxinsight.service.impl;

import io.ldxinsight.dto.AuthResponse;
import io.ldxinsight.dto.LoginRequest;
import io.ldxinsight.dto.RegisterRequest;
import io.ldxinsight.exception.DuplicateResourceException;
import io.ldxinsight.mapper.AuthMapper;
import io.ldxinsight.model.Role;
import io.ldxinsight.model.User;
import io.ldxinsight.repository.UserRepository;
import io.ldxinsight.service.AuthService;
import io.ldxinsight.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username đã tồn tại: " + request.getUsername());
        }

        User user = authMapper.toUser(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(Role.ROLE_USER);

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("Lỗi không tìm thấy user sau khi xác thực"));

        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }
}