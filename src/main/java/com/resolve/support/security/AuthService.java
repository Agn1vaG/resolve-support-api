package com.resolve.support.security;

import com.resolve.support.account.dto.UserResponse;
import com.resolve.support.account.entity.User;
import com.resolve.support.account.mapper.UserMapper;
import com.resolve.support.account.repository.UserRepository;
import com.resolve.support.account.service.UserService;
import com.resolve.support.security.dto.AuthResponse;
import com.resolve.support.security.dto.LoginRequest;
import com.resolve.support.security.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UserService userService, UserRepository userRepository, UserMapper userMapper,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        return userMapper.toResponse(userService.register(request));
    }

    public AuthResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserDetails details = (UserDetails) result.getPrincipal();
        User user = userRepository.findByEmail(details.getUsername()).orElseThrow();
        return new AuthResponse(jwtService.generateToken(details), "Bearer",
                jwtProperties.getExpirationMs() / 1000, userMapper.toResponse(user));
    }
}
