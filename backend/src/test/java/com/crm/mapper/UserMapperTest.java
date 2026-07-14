package com.crm.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.crm.dto.UserResponse;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.enums.RoleType;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void shouldMapUserToResponse() {
        Role role = new Role();
        role.setRoleType(RoleType.ADMIN);

        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUsername("jdoe");
        user.setEmail("john@example.com");
        user.setPhone("0123456789");
        user.setEnabled(true);
        user.setRole(role);

        UserResponse response = userMapper.toResponse(user);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getUsername()).isEqualTo("jdoe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getPhone()).isEqualTo("0123456789");
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getRoleType()).isEqualTo("ADMIN");
    }
}
