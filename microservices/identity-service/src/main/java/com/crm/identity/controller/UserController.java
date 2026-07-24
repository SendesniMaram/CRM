package com.crm.identity.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crm.identity.dto.UserResponse;
import com.crm.identity.service.IIdentityService;

/**
 * REST controller for user consultation endpoints.
 */
@RestController
@RequestMapping(path = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final IIdentityService identityService;

    public UserController(IIdentityService identityService) {
        this.identityService = identityService;
    }

    /**
     * GET /api/users — Retrieve all users.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = identityService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/{id} — Retrieve a single user by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = identityService.getUserById(id);
        return ResponseEntity.ok(user);
    }
}
