package com.crm.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.crm.entity.User;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.DepartmentRepository;
import com.crm.repository.UserRepository;
import com.crm.service.UserService;


@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public UserServiceImpl(UserRepository userRepository, DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }


    @Override
    public Page<User> searchUsers(Integer page, Integer size, String sortBy, String direction,
                                   String username, String email, String role, Boolean enabled) {
        Pageable pageable;

        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 10;
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        Sort.Direction dir = (direction != null && direction.equalsIgnoreCase("DESC")) ? Sort.Direction.DESC : Sort.Direction.ASC;
        pageable = org.springframework.data.domain.PageRequest.of(resolvedPage, resolvedSize, org.springframework.data.domain.Sort.by(dir, resolvedSortBy));

        Specification<User> spec = Specification.where(null);
        if (username != null && !username.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        if (enabled != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), enabled));
        }
        if (role != null && !role.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("role").get("roleType")), role.toUpperCase()));
        }

        return userRepository.findAll(spec, pageable);
    }

    @Override
    public Page<User> searchUsersByKeyword(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return userRepository.findAll(pageable);
        }
        String kw = keyword.toLowerCase();
        Specification<User> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.like(cb.lower(root.get("firstName")), "%" + kw + "%"));
            predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + kw + "%"));
            predicates.add(cb.like(cb.lower(root.get("username")), "%" + kw + "%"));
            predicates.add(cb.like(cb.lower(root.get("email")), "%" + kw + "%"));
            return cb.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));

        };
        return userRepository.findAll(spec, pageable);
    }


    @Override
    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public User getUserByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);

        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setPassword(userDetails.getPassword());
        user.setPhone(userDetails.getPhone());
        user.setEnabled(userDetails.isEnabled());
        user.setAccountNonLocked(userDetails.isAccountNonLocked());
        user.setRole(userDetails.getRole());

        // Department assignment is handled via UserRequest.departmentId mapping in UserMapper.
        // Here, we keep existing department on the entity.



        return userRepository.save(user);

    }


    @Override
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    @Override
    public User toggleUserStatus(Long id, boolean enabled) {
        User user = getUserById(id);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }
}
