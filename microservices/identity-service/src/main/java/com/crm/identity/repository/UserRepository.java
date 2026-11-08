package com.crm.identity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    boolean existsByRole_RoleTypeIn(java.util.Collection<com.crm.identity.enums.RoleType> roleTypes);
    long countByRole_RoleType(com.crm.identity.enums.RoleType roleType);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select count(u) from User u join u.role r where r.roleType = com.crm.identity.enums.RoleType.SUPER_ADMIN and u.enabled = true and u.accountNonLocked = true")
    long countUsableSuperAdmins();

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByRole(Role role);

    List<User> findByEnabled(boolean enabled);
}

