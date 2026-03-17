package com.trendzy.service;

import com.trendzy.exception.TrendZyException;
import com.trendzy.model.jpa.User;
import com.trendzy.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new TrendZyException("Current user not found"));
        }
        throw new TrendZyException("User not authenticated");
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new TrendZyException("User not found"));
    }

    public void toggleSaveProduct(String productId) {
        User user = getCurrentUser();
        List<String> savedTrends = user.getSavedTrendIds();
        if (savedTrends.contains(productId)) {
            savedTrends.remove(productId);
        } else {
            savedTrends.add(productId);
        }
        userRepository.save(user);
    }

    public boolean isProductSaved(String productId) {
        try {
            User user = getCurrentUser();
            return user.getSavedTrendIds().contains(productId) || user.getSavedCuratedIds().contains(productId);
        } catch (Exception e) {
            return false;
        }
    }
}
