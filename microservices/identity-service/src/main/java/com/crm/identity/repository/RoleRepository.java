package com.crm.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crm.identity.entity.Role;
import com.crm.identity.enums.RoleType;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Role r where r.roleType = :type")
    Optional<Role> lockByRoleType(@org.springframework.data.repository.query.Param("type") RoleType type);

    Optional<Role> findByName(String name);

    Optional<Role> findByRoleType(RoleType roleType);

    boolean existsByRoleType(RoleType roleType);
}

