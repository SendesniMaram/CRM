package com.crm.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.crm.dto.UserRequest;
import com.crm.dto.UserResponse;
import com.crm.entity.Role;
import com.crm.entity.User;

@Component
public class UserMapper {

    public User toEntity(UserRequest request, Role role) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());
        user.setRole(role);
        return user;
    }

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setEnabled(user.isEnabled());
        response.setRoleType(user.getRole() != null ? user.getRole().getRoleType().name() : null);
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public List<UserResponse> toResponseList(List<User> users) {
        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
