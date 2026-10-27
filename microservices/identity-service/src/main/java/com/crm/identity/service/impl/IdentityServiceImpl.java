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
import com.crm.identity.dto.AdminUserCreateRequest;
import com.crm.identity.dto.AdminUserUpdateRequest;
import com.crm.identity.dto.AdminUserRolesRequest;
import com.crm.identity.entity.Role;
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

    @Override
    @Transactional
    public UserResponse createAdminUser(AdminUserCreateRequest request, User actor) {
        requireAdminRole(actor, request.roleType());
        ensureUnique(request.username(), request.email(), null);
        User user = new User();
        user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
        user.setUsername(request.username().trim()); user.setEmail(request.email().trim());
        user.setPassword(passwordEncoder.encode(request.password())); user.setPhone(trimToNull(request.phone()));
        user.setEnabled(true); user.setAccountNonLocked(true);
        user.getRole().add(findRole(request.roleType()));
        return toUserResponse(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, AdminUserUpdateRequest request, User actor) {
        User user = getUser(id);
        requireCanManageTarget(actor, user);
        ensureUnique(user.getUsername(), request.email(), id);
        user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
        user.setEmail(request.email().trim()); user.setPhone(trimToNull(request.phone()));
        return toUserResponse(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserResponse updateUserRoles(Long id, AdminUserRolesRequest request, User actor) {
        User user = getUser(id);
        if (request.roles() == null || request.roles().isEmpty()) throw new IllegalArgumentException("At least one role is required");
        boolean superActor = actor.getRole().stream().anyMatch(role -> role.getRoleType() == RoleType.SUPER_ADMIN);
        if (!superActor && request.roles().stream().anyMatch(role -> role != RoleType.EMPLOYEE && role != RoleType.CLIENT)) {
            throw new org.springframework.security.access.AccessDeniedException("Only SUPER_ADMIN can assign administrative roles");
        }
        boolean targetSuper = user.getRole().stream().anyMatch(role -> role.getRoleType() == RoleType.SUPER_ADMIN);
        requireCanManageTarget(actor, user);
        if (targetSuper && !request.roles().contains(RoleType.SUPER_ADMIN)
                && userRepository.countByRole_RoleType(RoleType.SUPER_ADMIN) <= 1) {
            throw new IllegalStateException("The last SUPER_ADMIN cannot be removed");
        }
        user.getRole().clear();
        request.roles().stream().sorted().map(this::findRole).forEach(user.getRole()::add);
        return toUserResponse(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, boolean enabled, User actor) {
        User user = getUser(id);
        boolean targetSuper = user.getRole().stream().anyMatch(role -> role.getRoleType() == RoleType.SUPER_ADMIN);
        requireCanManageTarget(actor, user);
        if (!enabled && user.getId().equals(actor.getId())) throw new IllegalArgumentException("You cannot deactivate your own account");
        if (!enabled && targetSuper && userRepository.countByRole_RoleType(RoleType.SUPER_ADMIN) <= 1)
            throw new IllegalStateException("The last SUPER_ADMIN cannot be deactivated");
        user.setEnabled(enabled);
        return toUserResponse(userRepository.saveAndFlush(user));
    }

    private void requireAdminRole(User actor, RoleType requested) {
        boolean superActor = actor.getRole().stream().anyMatch(role -> role.getRoleType() == RoleType.SUPER_ADMIN);
        if (requested == RoleType.SUPER_ADMIN || requested == RoleType.ADMIN) {
            if (!superActor) throw new org.springframework.security.access.AccessDeniedException("Only SUPER_ADMIN can create administrative accounts");
        } else if (requested != RoleType.EMPLOYEE && requested != RoleType.CLIENT) {
            throw new IllegalArgumentException("Unsupported role");
        }
    }

    private void requireCanManageTarget(User actor, User target) {
        boolean superActor = actor.getRole().stream().anyMatch(role -> role.getRoleType() == RoleType.SUPER_ADMIN);
        boolean privilegedTarget = target.getRole().stream().anyMatch(role ->
                role.getRoleType() == RoleType.ADMIN || role.getRoleType() == RoleType.SUPER_ADMIN);
        if (!superActor && privilegedTarget) {
            throw new org.springframework.security.access.AccessDeniedException("Only SUPER_ADMIN can modify administrative accounts");
        }
    }

    private Role findRole(RoleType type) {
        return roleRepository.findByRoleType(type).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Requested role unavailable"));
    }

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private void ensureUnique(String username, String email, Long currentId) {
        userRepository.findByUsername(username).filter(user -> !user.getId().equals(currentId))
                .ifPresent(user -> { throw new IllegalStateException("Username already exists"); });
        userRepository.findByEmail(email).filter(user -> !user.getId().equals(currentId))
                .ifPresent(user -> { throw new IllegalStateException("Email already exists"); });
    }

    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

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
