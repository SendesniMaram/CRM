package com.crm.identity;

import com.crm.identity.config.SecurityConfig;
import com.crm.identity.controller.AccessTokenValidationController;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.security.AccessTokenValidationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AccessTokenValidationController.class,
        properties = "app.jwt.secret=synthetic-lot-a-outage-test-key-at-least-32-bytes")
@Import({SecurityConfig.class, AccessTokenValidationService.class})
class AccessTokenValidationAvailabilityTest {
    @Autowired MockMvc mvc;
    @MockBean UserRepository users;

    @Test void databaseUnavailableFailsClosedWithoutLeakingDiagnostics() throws Exception {
        String token = Jwts.builder().subject("synthetic-user").claim("uid", "101")
                .claim("enabled", true).claim("roles", List.of("CLIENT"))
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("synthetic-lot-a-outage-test-key-at-least-32-bytes"
                        .getBytes(StandardCharsets.UTF_8))).compact();
        when(users.findById(101L)).thenThrow(new DataAccessResourceFailureException("synthetic-private-db-diagnostics"));
        mvc.perform(get(AccessTokenValidationController.PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Access token validation unavailable"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("synthetic-private-db-diagnostics"))));
        verify(users).findById(101L);
    }
}
