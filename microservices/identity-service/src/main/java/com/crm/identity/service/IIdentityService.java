package com.crm.identity.service;

import java.util.List;

import com.crm.identity.dto.UserResponse;
import com.crm.identity.dto.RegisterRequest;
import com.crm.identity.dto.AdminUserCreateRequest;
import com.crm.identity.dto.AdminUserUpdateRequest;
import com.crm.identity.dto.AdminUserRolesRequest;
import com.crm.identity.entity.User;

/**
 * Contract for Identity business logic (user consultation).
 */
public interface IIdentityService {

    void createEmployee(RegisterRequest request);

    /**
     * Retrieve all users as DTOs.
     */
    List<UserResponse> getAllUsers();

    /**
     * Retrieve one user by its id.
     *
     * @param id user primary key
     * @return the found user
     * @throws com.crm.identity.exception.ResourceNotFoundException if not found
     */
    UserResponse getUserById(Long id);

    UserResponse createAdminUser(AdminUserCreateRequest request, User actor);
    UserResponse updateUser(Long id, AdminUserUpdateRequest request, User actor);
    UserResponse updateUserRoles(Long id, AdminUserRolesRequest request, User actor);
    UserResponse updateUserStatus(Long id, boolean enabled, User actor);
}
