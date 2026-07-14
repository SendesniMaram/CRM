package com.crm.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;

    public UserController(UserService userService, UserMapper userMapper, RoleRepository roleRepository) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
    }

    @Operation(summary = "Get Users (paginated)", description = "Retrieve users with pagination, sorting and optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<UserResponse>> getUsers(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") Integer page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") Integer size,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "id") String sortBy,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "ASC") String direction,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String username,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String role,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean enabled) {

        var userPage = userService.searchUsers(page, size, sortBy, direction, username, email, role, enabled);
        return ResponseEntity.ok(userPage.map(userMapper::toResponse));
    }

    @Operation(summary = "Search Users (global)", description = "Global search on firstName, lastName, username, email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<UserResponse>> searchUsers(
            @org.springframework.web.bind.annotation.RequestParam String keyword,

            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") Integer page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") Integer size,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "id") String sortBy,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "ASC") String direction) {

        Sort.Direction dir = direction != null && direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 10, Sort.by(dir, sortBy != null ? sortBy : "id"));
        var resultPage = userService.searchUsersByKeyword(keyword, pageable);
        return ResponseEntity.ok(resultPage.map(userMapper::toResponse));
    }


    @Operation(summary = "Get User By Id", description = "Retrieve one user by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @Operation(summary = "Create User", description = "Create a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        Role role = resolveRole(request.getRoleType());
        User user = userMapper.toEntity(request, role);
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(createdUser));
    }

    @Operation(summary = "Update User", description = "Update an existing user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        Role role = resolveRole(request.getRoleType());
        User userDetails = userMapper.toEntity(request, role);
        User updatedUser = userService.updateUser(id, userDetails);
        return ResponseEntity.ok(userMapper.toResponse(updatedUser));
    }

    @Operation(summary = "Delete User", description = "Delete an existing user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Enable User", description = "Enable a disabled user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User enabled successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> enableUser(@PathVariable Long id) {
        User user = userService.toggleUserStatus(id, true);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @Operation(summary = "Disable User", description = "Disable an active user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User disabled successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
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
