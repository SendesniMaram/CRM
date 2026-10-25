package com.crm.identity.security;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class UidIssuanceTest {
    private final JwtService jwt = new JwtService("synthetic-lot-b-issuance-key-at-least-32-bytes");

    @ParameterizedTest @NullSource @ValueSource(longs = {0, -1})
    void missingOrInvalidAuthenticatedIdCannotIssueAToken(Long id) {
        assertThatThrownBy(() -> jwt.generateToken(id, "user", "user@example.test", true, List.of("CLIENT")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest @ValueSource(longs = {101, Long.MAX_VALUE})
    void uidIsAStringRepresentingTheExactStableId(long id) {
        String token = jwt.generateToken(id, "user", "user@example.test", true, List.of("CLIENT"));
        assertThat(jwt.parseAccessToken(token).userId()).isEqualTo(id);
    }
}
