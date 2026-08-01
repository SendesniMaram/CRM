package com.crm.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crm.identity.dto.LoginRequest;
import com.crm.identity.dto.LoginResponse;
import com.crm.identity.dto.RefreshTokenRequest;
import com.crm.identity.dto.RefreshTokenResponse;
import com.crm.identity.dto.RegisterRequest;
import com.crm.identity.security.JwtService;
import com.crm.identity.service.IAuthenticationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthenticationController {

    private final IAuthenticationService authenticationService;

    public AuthenticationController(IAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }


    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authenticationService.login(request);

        JwtService jwtService = new JwtService();
        String token = jwtService.generateToken(
                loginResponse.getUsername(),
                loginResponse.getEmail(),
                loginResponse.isEnabled(),
                loginResponse.getRoles()
        );

        loginResponse.setToken(token);
        loginResponse.setType("Bearer");

        return ResponseEntity.ok(loginResponse);
    }


    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

}

