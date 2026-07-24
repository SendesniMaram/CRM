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

/**
 * Implementation of identity business logic (user consultation).
 */
@Service
@Transactional(readOnly = true)
public class IdentityServiceImpl implements IIdentityService {

    private final UserRepository userRepository;

    public IdentityServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        response.setRoleType(user.getRole().stream()
                .findFirst()
                .map(r -> r.getRoleType().name())
                .orElse(null));
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
