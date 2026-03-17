package com.trendzy.controller;

import com.trendzy.dto.request.LoginRequest;
import com.trendzy.dto.request.RegisterRequest;
import com.trendzy.dto.response.ApiResponse;
import com.trendzy.dto.response.AuthResponse;
import com.trendzy.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .data(response)
                .message("Registration successful")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .data(response)
                .message("Login successful")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
