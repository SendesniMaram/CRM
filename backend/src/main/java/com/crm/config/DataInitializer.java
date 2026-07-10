package com.crm.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.crm.entity.Role;
import com.crm.enums.RoleType;
import com.crm.repository.RoleRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        initializeRoleIfMissing(RoleType.SUPER_ADMIN);
        initializeRoleIfMissing(RoleType.ADMIN);
        initializeRoleIfMissing(RoleType.EMPLOYEE);
        initializeRoleIfMissing(RoleType.CLIENT);
    }

    private void initializeRoleIfMissing(RoleType roleType) {
        if (!roleRepository.existsByRoleType(roleType)) {
            Role role = new Role();
            role.setRoleType(roleType);
            roleRepository.save(role);
        }
    }
}
