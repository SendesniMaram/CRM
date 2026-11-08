package com.crm.identity.security;

import com.crm.identity.entity.*;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnifiedAuthenticationPolicyTest {
    static final String KEY = "synthetic-unified-policy-secret-at-least-32-bytes";
    static final String SID = "11111111-1111-4111-8111-111111111111";
    final UserRepository users = mock(UserRepository.class);
    final RefreshTokenRepository sessions = mock(RefreshTokenRepository.class);
    final JwtService jwt = new JwtService(KEY);
    final AccessTokenValidationService validator = new AccessTokenValidationService(jwt,users,sessions);
    User user;
    @BeforeEach void setup() {
        user = new User(); user.setId(42L); user.setUsername("worker"); user.getRole().add(new Role("EMPLOYEE",RoleType.EMPLOYEE));
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(sessions.hasActiveSession(eq(42L),eq(SID),any())).thenReturn(true);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    String token(String variant) {
        var builder = Jwts.builder().subject("worker").claim("uid","42").claim("sid",SID).claim("enabled",true)
                .claim("roles",List.of("EMPLOYEE")).expiration(Date.from(Instant.now().plusSeconds(60)));
        switch(variant) {
            case "missingUid" -> builder.claim("uid",null);
            case "wrongUid" -> builder.claim("uid","43");
            case "missingSid" -> builder.claim("sid",null);
            case "malformedSid" -> builder.claim("sid","not-a-session");
            case "expired" -> builder.expiration(Date.from(Instant.now().minusSeconds(1)));
            case "roleAdded" -> user.getRole().add(new Role("ADMIN",RoleType.ADMIN));
            case "roleRemoved" -> user.getRole().clear();
            case "disabled" -> user.setEnabled(false);
            case "locked" -> user.setAccountNonLocked(false);
            case "deleted" -> when(users.findById(42L)).thenReturn(Optional.empty());
            case "revoked" -> when(sessions.hasActiveSession(eq(42L),eq(SID),any())).thenReturn(false);
        }
        String key = variant.equals("signature") ? "foreign-signature-secret-at-least-32-bytes" : KEY;
        return builder.signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
    }
    void check(String bearer, int expected) throws Exception {
        for(boolean internal : new boolean[]{false,true}) {
            var request = new MockHttpServletRequest("GET",internal?"/api/internal/access-token/validate":"/api/users");
            request.addHeader("Authorization",bearer);
            var response = new MockHttpServletResponse();
            boolean[] reached = {false};
            var filter = internal ? new AccessTokenValidationFilter(validator,new ObjectMapper()) : new JwtAuthenticationFilter(validator,new ObjectMapper());
            filter.doFilter(request,response,(req,res)->reached[0]=true);
            assertEquals(expected==200,reached[0]); assertEquals(expected,response.getStatus());
            if(expected==200) assertEquals(List.of("ROLE_EMPLOYEE"),SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().map(a->a.getAuthority()).toList());
            else { assertNull(SecurityContextHolder.getContext().getAuthentication()); assertFalse(response.getContentAsString().contains("synthetic-private")); }
            SecurityContextHolder.clearContext();
        }
    }
    @Test void bothFiltersAcceptTheSameValidSignedIdentity() throws Exception { check("Bearer "+token("valid"),200); }
    @ParameterizedTest @ValueSource(strings={"missingUid","wrongUid","missingSid","malformedSid","expired","signature","roleAdded","roleRemoved","disabled","locked","deleted","revoked"})
    void bothFiltersRejectTheSameInvalidOrObsoleteToken(String state) throws Exception { check("Bearer "+token(state),401); }
    @Test void databaseFailureIsAnAvailabilityFailureWithoutDetailsOrAuthentication() throws Exception {
        when(users.findById(42L)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic-private SQL"));
        check("Bearer "+token("valid"),503);
    }
    @Test void malformedAuthorizationNeverRunsBusinessCode() throws Exception { check("Bearer invalid\r\nvalue",401); }
    @Test void publicLogoutDoesNotRequireAnAccessTokenThatMayAlreadyBeExpired() throws Exception {
        var request = new MockHttpServletRequest("POST","/api/auth/logout");
        boolean[] reached = {false}; new JwtAuthenticationFilter(validator,new ObjectMapper()).doFilter(request,new MockHttpServletResponse(),(r,s)->reached[0]=true);
        assertTrue(reached[0]); verifyNoInteractions(users,sessions);
    }
}
