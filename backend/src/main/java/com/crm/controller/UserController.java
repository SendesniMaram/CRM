package com.crm.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crm.dto.UserRequest;
import com.crm.dto.UserResponse;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.enums.RoleType;
import com.crm.mapper.UserMapper;
import com.crm.repository.RoleRepository;
import com.crm.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;

    public UserController(UserService userService, UserMapper userMapper, RoleRepository roleRepository) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userMapper.toResponseList(userService.getAllUsers());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        Role role = resolveRole(request.getRoleType());
        User user = userMapper.toEntity(request, role);
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(createdUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        Role role = resolveRole(request.getRoleType());
        User userDetails = userMapper.toEntity(request, role);
        User updatedUser = userService.updateUser(id, userDetails);
        return ResponseEntity.ok(userMapper.toResponse(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> enableUser(@PathVariable Long id) {
        User user = userService.toggleUserStatus(id, true);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> disableUser(@PathVariable Long id) {
        User user = userService.toggleUserStatus(id, false);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    private Role resolveRole(String roleType) {
        if (roleType == null || roleType.isBlank()) {
            return roleRepository.findByRoleType(RoleType.EMPLOYEE)
                    .orElseThrow(() -> new RuntimeException("Default role EMPLOYEE not found"));
        }

        try {
            RoleType parsedRoleType = RoleType.valueOf(roleType.toUpperCase());
            return roleRepository.findByRoleType(parsedRoleType)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleType));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid role type: " + roleType);
        }
    }
}
