package com.crm.identity.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.crm.identity.entity.Role;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.RoleRepository;

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
            Role role = new Role(roleType.name(), roleType);
            roleRepository.save(role);
        }
    }
}

