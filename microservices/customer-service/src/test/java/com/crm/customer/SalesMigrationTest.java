package com.crm.customer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class SalesMigrationTest {
    @Test
    void appliesContactAndSalesMigrationsToAnIsolatedMysqlModeDatabase() {
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
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        assertTrue(columnExists(jdbc, "CUSTOMERS", "CONTACT_TYPE"));
        assertTrue(columnExists(jdbc, "SALES_OPPORTUNITIES", "EXPECTED_CLOSE_DATE"));
        assertTrue(tableExists(jdbc, "CUSTOMER_INTERACTIONS"));
        assertTrue(tableExists(jdbc, "OPPORTUNITY_STAGE_HISTORY"));
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
