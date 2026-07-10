package com.crm.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.crm.entity.Role;
import com.crm.enums.RoleType;
import com.crm.repository.RoleRepository;
import com.crm.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Role getRoleById(Long id) {
        Optional<Role> role = roleRepository.findById(id);
        return role.orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
    }

    @Override
    public Role getRoleByType(RoleType roleType) {
        Optional<Role> role = roleRepository.findByRoleType(roleType);
        return role.orElseThrow(() -> new RuntimeException("Role not found with type: " + roleType));
    }

    @Override
    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public void deleteRole(Long id) {
        Role role = getRoleById(id);
        roleRepository.delete(role);
    }
}
