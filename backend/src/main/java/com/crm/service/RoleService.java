package com.crm.service;

import java.util.List;

import com.crm.entity.Role;
import com.crm.enums.RoleType;

public interface RoleService {

    List<Role> getAllRoles();

    Role getRoleById(Long id);

    Role getRoleByType(RoleType roleType);

    Role saveRole(Role role);

    void deleteRole(Long id);
}
