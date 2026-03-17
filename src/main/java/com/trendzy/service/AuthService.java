package com.trendzy.service;

import com.trendzy.dto.request.LoginRequest;
import com.trendzy.dto.request.RegisterRequest;
import com.trendzy.dto.response.AuthResponse;
import com.trendzy.exception.TrendZyException;
import com.trendzy.model.jpa.User;
import com.trendzy.repository.jpa.UserRepository;
import com.trendzy.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new TrendZyException("Email already in use");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .savedTrendIds(new ArrayList<>())
                .savedCuratedIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        String jwt = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return buildAuthResponse(user, jwt);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new TrendZyException("User not found"));

        String jwt = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return buildAuthResponse(user, jwt);
    }

    private AuthResponse buildAuthResponse(User user, String jwt) {
        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .savedProducts(user.getSavedTrendIds())
                .build();

        return AuthResponse.builder()
                .token(jwt)
                .user(userDto)
                .build();
    }
}
