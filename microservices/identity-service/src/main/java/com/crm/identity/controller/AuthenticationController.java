package com.crm.identity.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crm.identity.dto.LoginRequest;
import com.crm.identity.dto.RegisterRequest;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthenticationController {

    private final com.crm.identity.service.IAuthenticationService authenticationService;

    public AuthenticationController(com.crm.identity.service.IAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }


    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            com.crm.identity.dto.LoginResponse loginResponse = authenticationService.login(request);

            com.crm.identity.security.JwtService jwtService = new com.crm.identity.security.JwtService();
            String token = jwtService.generateToken(loginResponse.getUsername(), loginResponse.getEmail(), loginResponse.isEnabled());

            loginResponse.setToken(token);
            loginResponse.setType("Bearer");

            return ResponseEntity.ok(loginResponse);
        } catch (com.crm.identity.exception.InvalidCredentialsException e) {
            return ResponseEntity.status(401).body(invalidLoginBody());
        }
    }


    private Map<String, String> invalidLoginBody() {
        return Map.of(
                "message", "Invalid username/email or password"
        );
    }





    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        try {
            authenticationService.register(request);
            return ResponseEntity.status(201).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", "Username or email already exists"));
        }
    }

}

