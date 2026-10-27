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
import com.crm.identity.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.crm.identity.entity.User;
import com.crm.identity.dto.AdminUserCreateRequest;
import com.crm.identity.dto.AdminUserUpdateRequest;
import com.crm.identity.dto.AdminUserRolesRequest;
import com.crm.identity.dto.AdminUserStatusRequest;

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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createEmployee(@Valid @RequestBody RegisterRequest request) {
        identityService.createEmployee(request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping(path = "/admin", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> createAdminUser(@Valid @RequestBody AdminUserCreateRequest request,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.status(201).body(identityService.createAdminUser(request, actor));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest request, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(identityService.updateUser(id, request, actor));
    }

    @PutMapping(path = "/{id}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> updateRoles(@PathVariable Long id,
            @Valid @RequestBody AdminUserRolesRequest request, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(identityService.updateUserRoles(id, request, actor));
    }

    @PatchMapping(path = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
            @Valid @RequestBody AdminUserStatusRequest request, @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(identityService.updateUserStatus(id, request.enabled(), actor));
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
