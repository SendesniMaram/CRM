package com.crm.payroll;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class PayrollPublicationMigrationTest {
    @Test
    void migrationAddsPrivateDefaultAndCanBeAppliedRepeatedlyToAnExistingSchema() throws Exception {
        String url = "jdbc:h2:mem:payroll_migration;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            connection.createStatement().execute("create table payrolls(id bigint primary key, status varchar(50))");
            connection.createStatement().execute("insert into payrolls values(1, 'PAID')");
        }
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "")
                .locations("classpath:db/migration").baselineOnMigrate(true).baselineVersion("1").load();
        flyway.migrate(); flyway.migrate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var rows = connection.createStatement().executeQuery("select status, published from payrolls where id=1")) {
            assertTrue(rows.next()); assertEquals("PAID", rows.getString("status")); assertFalse(rows.getBoolean("published"));
        }
    }
}
