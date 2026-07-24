package com.crm.identity.service;

import java.util.List;

import com.crm.identity.dto.UserResponse;

/**
 * Contract for Identity business logic (user consultation).
 */
public interface IIdentityService {

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
}
