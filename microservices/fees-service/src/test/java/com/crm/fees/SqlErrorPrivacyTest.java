package com.crm.fees;

import com.crm.fees.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Standalone MVC only: does not start a Spring Boot context, server or database. */
class SqlErrorPrivacyTest {
    @RestController static class FailureController {
        @GetMapping("/integrity") void integrity() { throw new DataIntegrityViolationException("SQL insert into secret_table password=PRIVATE constraint fk_private", new java.sql.SQLException("PRIVATE SQL detail")); }
        @GetMapping("/database") void database() { throw new DataAccessResourceFailureException("SELECT PRIVATE FROM secret_table password=PRIVATE"); }
    }
    @Test void persistenceResponsesNeverContainSqlConstraintsOrValues() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailureController()).setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String path : new String[]{"/integrity", "/database"}) {
            var response = mvc.perform(get(path)).andReturn().getResponse();
            assertTrue(response.getStatus() == 409 || response.getStatus() == 500);
            String body = response.getContentAsString();
            assertFalse(body.isEmpty());
            for (String forbidden : new String[]{"PRIVATE", "secret_table", "fk_private", "SELECT", "insert into", "password="})
                assertFalse(body.contains(forbidden), "Sensitive persistence detail in response");
        }
    }
}
