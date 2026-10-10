package com.crm.identity;

import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.name=application-test", "spring.profiles.active=test",
        "eureka.client.enabled=false", "spring.cloud.service-registry.auto-registration.enabled=false",
        "app.bootstrap.admin.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class AdminEmployeeCreationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;

    private Map<String, String> payload() {
        return Map.of("firstName", "Test", "lastName", "Employee", "username", "created-employee",
                "email", "created-employee@example.test", "password", "test-password-only",
                "phone", "123456", "role", "SUPER_ADMIN", "roles", "SUPER_ADMIN");
    }

    private String authorization(RoleType role) {
        User caller = new User();
        caller.setUsername("caller-" + role);
        caller.setEmail("caller-" + role + "@example.test");
        caller.setPassword(encoder.encode("test-password-only"));
        caller.setEnabled(true);
        caller.setAccountNonLocked(true);
        caller.getRole().add(roles.findByRoleType(role).orElseThrow());
        users.saveAndFlush(caller);
        return "Bearer " + jwt.generateToken(caller.getUsername(), caller.getEmail(), true);
    }

    @ParameterizedTest
    @EnumSource(value = RoleType.class, names = {"ADMIN", "SUPER_ADMIN"})
    void administratorsCreateOnlyEmployeeWithHashedPassword(RoleType role) throws Exception {
        mvc.perform(post("/api/users").header("Authorization", authorization(role))
                .contentType("application/json").content(json.writeValueAsString(payload())))
                .andExpect(status().isCreated()).andExpect(content().string(""));
        User created = users.findByUsername("created-employee").orElseThrow();
        assertThat(created.getRole()).extracting(r -> r.getRoleType()).containsExactly(RoleType.EMPLOYEE);
        assertThat(encoder.matches("test-password-only", created.getPassword())).isTrue();
        assertThat(created.getPassword().equals("test-password-only")).isFalse();
        assertThat(created.isEnabled()).isTrue();
        assertThat(created.isAccountNonLocked()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = RoleType.class, names = {"EMPLOYEE", "CLIENT"})
    void nonAdministratorsCannotCreate(RoleType role) throws Exception {
        mvc.perform(post("/api/users").header("Authorization", authorization(role))
                .contentType("application/json").content(json.writeValueAsString(payload())))
                .andExpect(status().isForbidden());
        assertThat(users.existsByUsername("created-employee")).isFalse();
    }

    @Test
    void anonymousCannotCreate() throws Exception {
        mvc.perform(post("/api/users").contentType("application/json")
                .content(json.writeValueAsString(payload()))).andExpect(status().isForbidden());
        assertThat(users.existsByUsername("created-employee")).isFalse();
    }

    @Test
    void duplicateUsernameReturnsConflict() throws Exception {
        duplicate("username");
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        duplicate("email");
    }

    private void duplicate(String field) throws Exception {
        String auth = authorization(RoleType.ADMIN);
        mvc.perform(post("/api/users").header("Authorization", auth).contentType("application/json")
                .content(json.writeValueAsString(payload()))).andExpect(status().isCreated());
        var request = new java.util.HashMap<>(payload());
        request.put(field.equals("username") ? "email" : "username", "other@example.test");
        long count = users.count();
        mvc.perform(post("/api/users").header("Authorization", auth).contentType("application/json")
                .content(json.writeValueAsString(request))).andExpect(status().isConflict());
        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void invalidPayloadReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/users").header("Authorization", authorization(RoleType.ADMIN))
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(users.existsByUsername("created-employee")).isFalse();
    }

    @Test
    void publicRegistrationStillForcesClient() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(json.writeValueAsString(payload())))
                .andExpect(status().isCreated()).andExpect(content().string(""));
        assertThat(users.findByUsername("created-employee").orElseThrow().getRole())
                .extracting(r -> r.getRoleType()).containsExactly(RoleType.CLIENT);
    }
}
