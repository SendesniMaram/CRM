package com.crm.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.crm.entity.User;


public interface UserService {

    Page<User> searchUsers(Integer page, Integer size, String sortBy, String direction,
                             String username, String email, String role, Boolean enabled);

    Page<User> searchUsersByKeyword(String keyword, Pageable pageable);

    User getUserById(Long id);


    User getUserByEmail(String email);

    User getUserByUsername(String username);

    User createUser(User user);

    User updateUser(Long id, User userDetails);


    void deleteUser(Long id);

    User toggleUserStatus(Long id, boolean enabled);
}
