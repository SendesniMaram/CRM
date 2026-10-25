package com.crm.identity;

import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.RefreshTokenRepository;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.name=application-test", "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:lot_a_validation;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "eureka.client.enabled=false", "spring.cloud.service-registry.auto-registration.enabled=false",
        "app.bootstrap.admin.enabled=false"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class AccessTokenValidationIntegrationTest {
    static final String PATH = "/api/internal/access-token/validate";
    static final String KEY = "test-only-secret-key-not-for-production-crm2024";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired RefreshTokenRepository refreshTokens;

    // No enclosing test transaction: status updates commit before the next HTTP request.
    @BeforeEach void clean() { refreshTokens.deleteAll(); users.deleteAll(); }

    User user(RoleType role) {
        User user = new User(); user.setUsername("lot-a-user"); user.setEmail("lot-a@example.test");
        user.setPassword("unused-synthetic-password");
        user.getRole().add(roles.findByRoleType(role).orElseThrow());
        return users.saveAndFlush(user);
    }

    String token(User user, List<String> signedRoles) {
        return Jwts.builder().subject(user.getUsername()).claim("uid", user.getId().toString())
                .claim("enabled", true).claim("roles", signedRoles)
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8))).compact();
    }

    void check(String token, int status) throws Exception {
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().is(status)).andExpect(header().string("Cache-Control", "no-store"));
    }

    @ParameterizedTest @EnumSource(RoleType.class)
    void eachRoleCanValidateItsOwnTokenWithoutReadingPersonalDataOrWriting(RoleType role) throws Exception {
        User user = user(role);
        var before = users.findById(user.getId()).orElseThrow().getUpdatedAt();
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token(user, List.of(role.name()))))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(header().string("Cache-Control", "no-store"));
        assertThat(users.findById(user.getId()).orElseThrow().getUpdatedAt()).isEqualTo(before);
        assertThat(refreshTokens.count()).isZero();
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void statusChangeIsCheckedOnEveryRequestAndReactivationIsAccepted(boolean disable) throws Exception {
        User user = user(RoleType.EMPLOYEE);
        String token = token(user, List.of("EMPLOYEE"));
        check(token, 204);
        if (disable) user.setEnabled(false); else user.setAccountNonLocked(false);
        users.saveAndFlush(user);
        check(token, 401);
        user.setEnabled(true); user.setAccountNonLocked(true); users.saveAndFlush(user);
        check(token, 204);
    }

    @Test void removedRoleInvalidatesThePreviouslyIssuedToken() throws Exception {
        User user = user(RoleType.ADMIN);
        String token = token(user, List.of("ROLE_ADMIN"));
        check(token, 204);
        user.getRole().clear(); user.getRole().add(roles.findByRoleType(RoleType.EMPLOYEE).orElseThrow());
        users.saveAndFlush(user);
        check(token, 401);
    }

    @Test void deletedUserCannotBeReplacedByAReusedUsername() throws Exception {
        User original = user(RoleType.CLIENT);
        String token = token(original, List.of("CLIENT"));
        users.deleteById(original.getId());
        User replacement = user(RoleType.CLIENT);
        assertThat(replacement.getId()).isNotEqualTo(original.getId());
        check(token, 401);
    }

    @Test void uidMustMatchTheSignedUsernameAndQueryParametersCannotSelectAnotherAccount() throws Exception {
        User first = user(RoleType.CLIENT);
        String valid = token(first, List.of("CLIENT"));
        first.setUsername("different-user"); users.saveAndFlush(first);
        check(valid, 401);
        mvc.perform(get(PATH).param("userId", "99999").param("email", "other@example.test")
                .header("Authorization", "Bearer " + token(first, List.of("CLIENT"))))
                .andExpect(status().isNoContent());
    }

    @Test void anonymousMalformedAndLegacyTokensAreRejected() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        check("malformed", 401);
        User user = user(RoleType.ADMIN);
        String legacy = Jwts.builder().subject(user.getUsername()).claim("enabled", true).claim("roles", List.of("ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8))).compact();
        check(legacy, 401);
    }

    @Test void authenticationDoesNotGrantOtherInternalMethodsOrPaths() throws Exception {
        User user = user(RoleType.SUPER_ADMIN);
        String token = token(user, List.of("SUPER_ADMIN"));
        mvc.perform(post(PATH).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/internal/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
