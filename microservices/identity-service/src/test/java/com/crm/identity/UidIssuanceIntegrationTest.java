package com.crm.identity;

import com.crm.identity.entity.RefreshToken;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.RefreshTokenRepository;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.name=application-test", "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:lot_b_issuance;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "eureka.client.enabled=false", "spring.cloud.service-registry.auto-registration.enabled=false",
        "app.bootstrap.admin.enabled=false"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class UidIssuanceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;

    @BeforeEach void clean() { tokens.deleteAll(); users.deleteAll(); }

    User user(String name, RoleType role) {
        User user = new User(); user.setUsername(name); user.setEmail(name + "@example.test");
        user.setPassword(encoder.encode("synthetic-password"));
        user.getRole().add(roles.findByRoleType(role).orElseThrow());
        return users.saveAndFlush(user);
    }

    JsonNode login(User user, boolean email) throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of(email ? "email" : "username",
                        email ? user.getEmail() : user.getUsername(), "password", "synthetic-password",
                        "uid", "999999", "authenticatedUserId", 999999, "employeeId", 777777, "customerId", 888888))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    JsonNode refresh(String value) throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content(json.writeValueAsString(Map.of("refreshToken", value, "uid", "999999"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    void assertContract(JsonNode response, String... fields) {
        List<String> actual = new ArrayList<>(); response.fieldNames().forEachRemaining(actual::add);
        assertThat(actual).containsExactlyInAnyOrder(fields);
        assertThat(response.get("expiration").asLong()).isEqualTo(3600000L);
        assertThat(response.get("type").asText()).isEqualTo("Bearer");
    }

    void assertToken(String token, User user, List<String> expectedRoles) throws Exception {
        var parsed = jwt.parseAccessToken(token);
        assertThat(parsed.userId()).isEqualTo(user.getId());
        assertThat(parsed.username()).isEqualTo(user.getUsername());
        assertThat(parsed.roles()).containsExactlyInAnyOrderElementsOf(expectedRoles);
        var raw = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(
                "test-only-secret-key-not-for-production-crm2024".getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload();
        assertThat(raw.get("uid")).isEqualTo(user.getId().toString());
        assertThat(raw.getSubject()).isEqualTo(user.getUsername());
        assertThat(raw.get("email")).isEqualTo(user.getEmail());
        assertThat(raw.get("enabled")).isEqualTo(true);
        assertThat(raw.getExpiration().getTime() - raw.getIssuedAt().getTime()).isEqualTo(3600000L);
        mvc.perform(get("/api/internal/access-token/validate").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    static Stream<Arguments> loginModes() {
        return Stream.of(RoleType.values()).flatMap(role -> Stream.of(Arguments.of(role, false), Arguments.of(role, true)));
    }

    @ParameterizedTest @MethodSource("loginModes")
    void loginAndRefreshUseAuthenticatedUserIdAndPreserveContracts(RoleType role, boolean email) throws Exception {
        User other = user("other-account", RoleType.CLIENT);
        User user = user("authenticated-account", role);
        assertThat(user.getId()).isNotEqualTo(other.getId());
        LocalDateTime before = LocalDateTime.now();
        JsonNode login = login(user, email);
        assertContract(login, "username", "email", "enabled", "token", "refreshToken", "type", "role", "roles", "expiration");
        assertToken(login.get("token").asText(), user, List.of(role.name()));
        String original = login.get("refreshToken").asText();
        assertThat(tokens.findByToken(original).orElseThrow().getExpiryDate())
                .isBetween(before.plusDays(7).minusSeconds(1), LocalDateTime.now().plusDays(7).plusSeconds(1));
        JsonNode refreshed = refresh(original);
        assertContract(refreshed, "accessToken", "refreshToken", "type", "roles", "expiration");
        assertToken(refreshed.get("accessToken").asText(), user, List.of(role.name()));
        assertThat(tokens.findByToken(original).orElseThrow().isRevoked()).isTrue();
        assertThat(refreshed.get("refreshToken").asText()).isNotEqualTo(original);
        assertThat(tokens.count()).isEqualTo(2);
        mvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content(json.writeValueAsString(Map.of("refreshToken", original))))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void inactiveAccountCannotLoginOrRefreshAfterStatusChange(boolean disable) throws Exception {
        User user = user("status-account", RoleType.EMPLOYEE);
        JsonNode login = login(user, false);
        if (disable) user.setEnabled(false); else user.setAccountNonLocked(false);
        users.saveAndFlush(user);
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("username", user.getUsername(), "password", "synthetic-password"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content(json.writeValueAsString(Map.of("refreshToken", login.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized());
        assertThat(tokens.count()).isEqualTo(1);
        assertThat(tokens.findByToken(login.get("refreshToken").asText()).orElseThrow().isRevoked()).isTrue();
    }

    @Test void existingRefreshSessionCanUpgradeALegacyAccessTokenWithoutUid() throws Exception {
        User user = user("legacy-account", RoleType.CLIENT);
        RefreshToken legacy = new RefreshToken(); legacy.setToken("synthetic-legacy-refresh");
        legacy.setUser(user); legacy.setExpiryDate(LocalDateTime.now().plusDays(1)); tokens.saveAndFlush(legacy);
        String old = jwt.generateToken(user.getUsername(), user.getEmail(), true, List.of("CLIENT"));
        mvc.perform(get("/api/internal/access-token/validate").header("Authorization", "Bearer " + old))
                .andExpect(status().isUnauthorized());
        assertToken(refresh(legacy.getToken()).get("accessToken").asText(), user, List.of("CLIENT"));
    }

    @Test void refreshKeepsStableIdAndUsesCurrentRoles() throws Exception {
        User user = user("roles-account", RoleType.ADMIN);
        JsonNode login = login(user, false);
        user.getRole().clear(); user.getRole().add(roles.findByRoleType(RoleType.EMPLOYEE).orElseThrow());
        user.getRole().add(roles.findByRoleType(RoleType.CLIENT).orElseThrow()); users.saveAndFlush(user);
        JsonNode result = refresh(login.get("refreshToken").asText());
        assertToken(result.get("accessToken").asText(), user, List.of("EMPLOYEE", "CLIENT"));
    }
}
