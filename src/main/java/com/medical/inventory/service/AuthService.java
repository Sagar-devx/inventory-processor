package com.medical.inventory.service;

import com.medical.inventory.dto.AuthResponse;
import com.medical.inventory.dto.LoginRequest;
import com.medical.inventory.dto.RegisterRequest;
import com.medical.inventory.entity.AppUser;
import com.medical.inventory.entity.Role;
import com.medical.inventory.exception.ProcessingException;
import com.medical.inventory.repository.UserRepository;
import com.medical.inventory.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String token = tokenProvider.generateToken(authentication);
        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ProcessingException("User not found after authentication"));

        return AuthResponse.of(token, user.getUsername(), user.getRole().name(), 86400000L);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' is already registered");
        }

        Role role = request.role() != null ? request.role() : Role.ROLE_USER;
        AppUser newUser = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.email(),
                role
        );

        userRepository.save(newUser);

        String token = tokenProvider.generateToken(newUser.getUsername(), newUser.getRole().name());
        return AuthResponse.of(token, newUser.getUsername(), newUser.getRole().name(), 86400000L);
    }
}
