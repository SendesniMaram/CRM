package com.crm.identity.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crm.identity.dto.UserResponse;
import com.crm.identity.entity.User;
import com.crm.identity.exception.ResourceNotFoundException;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.service.IIdentityService;
import com.crm.identity.dto.RegisterRequest;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Implementation of identity business logic (user consultation).
 */
@Service
@Transactional(readOnly = true)
public class IdentityServiceImpl implements IIdentityService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void createEmployee(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())
                || userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Username or email already exists");
        }
        var role = roleRepository.findByRoleType(RoleType.EMPLOYEE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Employee role unavailable"));
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.getRole().add(role);
        userRepository.saveAndFlush(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toUserResponse(user);
    }

    /**
     * Map a User entity to a UserResponse DTO.
     */
    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setEnabled(user.isEnabled());
        response.setAccountNonLocked(user.isAccountNonLocked());
        // Explicit directory order, independent of Set/JPA iteration and enum ordinals.
        List<RoleType> order = List.of(RoleType.SUPER_ADMIN, RoleType.ADMIN, RoleType.EMPLOYEE, RoleType.CLIENT);
        List<String> roles = order.stream()
                .filter(type -> user.getRole().stream().anyMatch(role -> type == role.getRoleType()))
                .map(Enum::name).toList();
        response.setRoles(roles);
        response.setRoleType(roles.isEmpty() ? null : roles.get(0));
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
