package com.crm.customer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class SalesMigrationTest {
    @Test
    void appliesContactAndSalesMigrationsToAdaptedH2Copies() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:sales-migration;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE customers (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    phone VARCHAR(50),
                    company VARCHAR(255),
                    address VARCHAR(500),
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);

        Flyway.configure()
                .dataSource(dataSource)
                .locations(H2MigrationCopies.location())
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        assertTrue(columnExists(jdbc, "CUSTOMERS", "CONTACT_TYPE"));
        assertTrue(columnExists(jdbc, "SALES_OPPORTUNITIES", "EXPECTED_CLOSE_DATE"));
        assertTrue(tableExists(jdbc, "CUSTOMER_INTERACTIONS"));
        assertTrue(tableExists(jdbc, "OPPORTUNITY_STAGE_HISTORY"));
        assertTrue(columnExists(jdbc, "CUSTOMERS", "ARCHIVED"));
        assertTrue(columnExists(jdbc, "CUSTOMER_INTERACTIONS", "EMPLOYEE_VISIBLE"));
        assertTrue(tableExists(jdbc, "CLIENT_CASES"));
        assertTrue(tableExists(jdbc, "CLIENT_CASE_MESSAGES"));
        assertTrue(tableExists(jdbc, "CLIENT_CASE_EVENTS"));
    }

    @Test
    void h2AdaptationPreservesQuotedCommasAndCompositeConstraints() {
        String source="ALTER TABLE sample ADD COLUMN label VARCHAR(50) DEFAULT 'a,b', ADD CONSTRAINT uk_sample UNIQUE(label,id), ADD INDEX idx_sample(label,id);";
        String adapted=H2MigrationCopies.adapt("\uFEFF-- baseline; comment only\n"+source);
        org.junit.jupiter.api.Assertions.assertEquals("ALTER TABLE sample ADD COLUMN label VARCHAR(50) DEFAULT 'a,b';\nALTER TABLE sample ADD CONSTRAINT uk_sample UNIQUE(label,id);\nCREATE INDEX idx_sample ON sample(label,id);\n",adapted);
    }

    private boolean columnExists(JdbcTemplate jdbc, String table, String column) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, table, column) > 0;
    }

    private boolean tableExists(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?
                """, Integer.class, table) > 0;
    }
}
