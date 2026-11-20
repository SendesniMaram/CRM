package com.crm.hr;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HrMigrationTest {
    @Test void createsNewHrTablesOnAnIsolatedEmptySchema() throws Exception {
        String url="jdbc:h2:mem:hr_migrations;MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration").load().migrate();
        try(var connection=DriverManager.getConnection(url,"sa","");var statement=connection.createStatement();var rows=statement.executeQuery(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME IN ('EMPLOYEE_CONTRACTS','EMPLOYEE_EVALUATIONS','JOB_OFFERS','JOB_APPLICATIONS','EMPLOYEE_DOCUMENTS','EMPLOYEE_CAREER_EVENTS','LEAVE_QUOTAS')")) {
            rows.next();assertTrue(rows.getInt(1)>=7);
        }
    }
}
