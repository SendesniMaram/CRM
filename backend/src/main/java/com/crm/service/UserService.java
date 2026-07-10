package com.crm.service;

import java.util.List;

import com.crm.entity.User;

public interface UserService {

    List<User> getAllUsers();

    User getUserById(Long id);

    User getUserByEmail(String email);

    User getUserByUsername(String username);

    User createUser(User user);

    User updateUser(Long id, User userDetails);

    void deleteUser(Long id);

    User toggleUserStatus(Long id, boolean enabled);
}
