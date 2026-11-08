package com.crm.invoice;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class QuoteMigrationTest {
    @Test
    void appliesInvoiceLifecycleMigrationsToAdaptedH2Copies() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:quote-migration;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway.configure()
                .dataSource(dataSource)
                .locations(H2MigrationCopies.location())
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertTrue(tableExists(jdbc, "QUOTES"));
        assertTrue(tableExists(jdbc, "QUOTE_LINES"));
        assertTrue(tableExists(jdbc, "INVOICE_LINES"));
        assertTrue(tableExists(jdbc, "INVOICE_PAYMENTS"));
        assertTrue(tableExists(jdbc, "INVOICE_REMINDERS"));
        assertTrue(tableExists(jdbc, "INVOICE_NUMBER_SEQUENCE"));
        assertTrue(tableExists(jdbc, "INVOICE_PRESENTATIONS"));
        assertTrue(tableExists(jdbc, "INVOICE_DOCUMENTS"));
        assertTrue(tableExists(jdbc, "INVOICE_RECONCILIATIONS"));
        assertTrue(tableExists(jdbc, "INVOICE_FEE_CLAIMS"));
        assertTrue(tableExists(jdbc, "INVOICE_REMINDER_TASKS"));
        org.junit.jupiter.api.Assertions.assertFalse(jdbc.queryForObject("select enabled from invoice_reminder_policy where id=1",Boolean.class));
    }

    private boolean tableExists(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?
                """, Integer.class, table) > 0;
    }
}
