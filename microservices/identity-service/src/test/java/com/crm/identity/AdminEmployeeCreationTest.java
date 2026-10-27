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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @ParameterizedTest @EnumSource(RoleType.class)
    void directoryRolesContractDoesNotBroadenReadPermissions(RoleType role) throws Exception {
        String bearer = authorization(role);
        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users")
                .header("Authorization", bearer));
        if (role == RoleType.ADMIN || role == RoleType.SUPER_ADMIN) {
            result.andExpect(status().isOk()).andExpect(jsonPath("$[0].roles").isArray())
                    .andExpect(jsonPath("$[0].accountNonLocked").isBoolean())
                    .andExpect(jsonPath("$[0].password").doesNotExist())
                    .andExpect(jsonPath("$[0].refreshToken").doesNotExist());
        } else {
            result.andExpect(status().isForbidden());
        }
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

    @Test
    void superAdminCanCreateClientAndAdminCannotEscalateRoles() throws Exception {
        var body = Map.of("firstName", "Test", "lastName", "Client", "username", "created-client",
                "email", "created-client@example.test", "password", "test-password-only", "roleType", "CLIENT");
        mvc.perform(post("/api/users/admin").header("Authorization", authorization(RoleType.SUPER_ADMIN))
                .contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.roles[0]").value("CLIENT"));
        User created = users.findByUsername("created-client").orElseThrow();
        assertThat(encoder.matches("test-password-only", created.getPassword())).isTrue();
        var adminBody = new java.util.HashMap<>(body); adminBody.put("username", "forbidden-admin");
        adminBody.put("email", "forbidden-admin@example.test"); adminBody.put("roleType", "ADMIN");
        mvc.perform(post("/api/users/admin").header("Authorization", authorization(RoleType.ADMIN))
                .contentType("application/json").content(json.writeValueAsString(adminBody))).andExpect(status().isForbidden());
        assertThat(users.existsByUsername("forbidden-admin")).isFalse();
    }

    @Test
    void adminCanEditReactivateAndAssignOnlyBusinessRoles() throws Exception {
        String bearer = authorization(RoleType.ADMIN);
        mvc.perform(post("/api/users").header("Authorization", bearer).contentType("application/json")
                .content(json.writeValueAsString(payload()))).andExpect(status().isCreated());
        User employee = users.findByUsername("created-employee").orElseThrow();
        mvc.perform(put("/api/users/{id}", employee.getId()).header("Authorization", bearer).contentType("application/json")
                .content("{\"firstName\":\"Edited\",\"lastName\":\"Employee\",\"email\":\"edited@example.test\",\"phone\":\"555\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Edited"));
        mvc.perform(patch("/api/users/{id}/status", employee.getId()).header("Authorization", bearer).contentType("application/json")
                .content("{\"enabled\":false}" )).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(patch("/api/users/{id}/status", employee.getId()).header("Authorization", bearer).contentType("application/json")
                .content("{\"enabled\":true}" )).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        mvc.perform(put("/api/users/{id}/roles", employee.getId()).header("Authorization", bearer).contentType("application/json")
                .content("{\"roles\":[\"CLIENT\"]}" )).andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("CLIENT"));
        mvc.perform(put("/api/users/{id}/roles", employee.getId()).header("Authorization", bearer).contentType("application/json")
                .content("{\"roles\":[\"SUPER_ADMIN\"]}" )).andExpect(status().isForbidden());
        assertThat(users.findById(employee.getId()).orElseThrow().getRole()).extracting(r -> r.getRoleType()).containsExactly(RoleType.CLIENT);
    }

    @Test
    void adminCannotModifyExistingAdministrativeAccounts() throws Exception {
        var request = Map.of("firstName", "Managed", "lastName", "Admin", "username", "managed-admin",
                "email", "managed-admin@example.test", "password", "test-password-only", "roleType", "ADMIN");
        mvc.perform(post("/api/users/admin").header("Authorization", authorization(RoleType.SUPER_ADMIN))
                .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isCreated());
        User target = users.findByUsername("managed-admin").orElseThrow();
        String admin = authorization(RoleType.ADMIN);
        mvc.perform(put("/api/users/{id}", target.getId()).header("Authorization", admin).contentType("application/json")
                .content("{\"firstName\":\"Changed\",\"lastName\":\"Admin\",\"email\":\"changed@example.test\",\"phone\":\"\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/users/{id}/status", target.getId()).header("Authorization", admin).contentType("application/json")
                .content("{\"enabled\":false}" )).andExpect(status().isForbidden());
        assertThat(users.findById(target.getId()).orElseThrow().isEnabled()).isTrue();
    }
}
