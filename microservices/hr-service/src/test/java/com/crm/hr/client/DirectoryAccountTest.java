package com.crm.hr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class DirectoryAccountTest {
    final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest @ValueSource(strings = {"[\"EMPLOYEE\"]", "[\"ADMIN\",\"EMPLOYEE\"]",
            "[\"EMPLOYEE\",\"ADMIN\"]", "[\"EMPLOYEE\",\"EMPLOYEE\"]"})
    void acceptsMembershipRegardlessOfOrderDuplicatesOrLegacyRole(String roles) throws Exception {
        assertTrue(DirectoryAccount.eligibleEmployee(json.readTree("{\"roleType\":\"CLIENT\",\"roles\":" + roles
                + ",\"enabled\":true,\"accountNonLocked\":true}")));
    }

    @ParameterizedTest @ValueSource(strings = {
        "{\"roles\":[\"CLIENT\"],\"enabled\":true,\"accountNonLocked\":true}",
        "{\"roles\":[],\"enabled\":true,\"accountNonLocked\":true}",
        "{\"roles\":[\"EMPLOYEE\"],\"enabled\":false,\"accountNonLocked\":true}",
        "{\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":false}"})
    void rejectsIneligibleAccounts(String body) throws Exception {
        assertFalse(DirectoryAccount.eligibleEmployee(json.readTree(body)));
    }

    @ParameterizedTest @ValueSource(strings = {"null", "[]", "{}",
        "{\"roleType\":\"EMPLOYEE\",\"enabled\":true,\"accountNonLocked\":true}",
        "{\"roles\":\"EMPLOYEE\",\"enabled\":true,\"accountNonLocked\":true}",
        "{\"roles\":[\"EMPLOYEE\",null],\"enabled\":true,\"accountNonLocked\":true}",
        "{\"roles\":[\"UNKNOWN\"],\"enabled\":true,\"accountNonLocked\":true}",
        "{\"roles\":[\"EMPLOYEE\"],\"enabled\":\"true\",\"accountNonLocked\":true}",
        "{\"roles\":[\"EMPLOYEE\"],\"enabled\":true}"})
    void malformedResponseFailsClosedWithoutLegacyFallback(String body) throws Exception {
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> DirectoryAccount.eligibleEmployee(json.readTree(body))).getStatusCode().value());
    }
}
