package com.trendzy.controller;

import com.trendzy.dto.response.ApiResponse;
import com.trendzy.dto.response.AuthResponse;
import com.trendzy.dto.response.CuratedResponse;
import com.trendzy.dto.response.TrendResponse;
import com.trendzy.model.jpa.User;
import com.trendzy.service.CuratedService;
import com.trendzy.service.TrendService;
import com.trendzy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TrendService trendService;
    private final CuratedService curatedService;

    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserDto> getCurrentUser() {
        User user = userService.getCurrentUser();
        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .savedProducts(user.getSavedTrendIds())
                .build();
                
        return ApiResponse.<AuthResponse.UserDto>builder()
                .success(true)
                .data(userDto)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PostMapping("/save/{productId}")
    public ApiResponse<Void> saveProduct(@PathVariable String productId) {
        userService.toggleSaveProduct(productId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Product saved status toggled")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @DeleteMapping("/save/{productId}")
    public ApiResponse<Void> removeSavedProduct(@PathVariable String productId) {
        userService.toggleSaveProduct(productId);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Product removed successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping("/saved")
    public ApiResponse<List<Object>> getSavedProducts() {
        User user = userService.getCurrentUser();
        List<Object> mixed = new ArrayList<>();
        
        for (String trendId : user.getSavedTrendIds()) {
            try {
                TrendResponse tr = trendService.getTrendById(trendId);
                mixed.add(tr);
            } catch (Exception ignored) {}
        }
        for (String curatedId : user.getSavedCuratedIds()) {
            try {
                CuratedResponse cr = curatedService.getCuratedProductById(curatedId);
                mixed.add(cr);
            } catch (Exception ignored) {}
        }
        
        return ApiResponse.<List<Object>>builder()
                .success(true)
                .data(mixed)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
