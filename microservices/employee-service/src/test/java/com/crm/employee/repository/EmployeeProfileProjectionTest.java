package com.crm.employee.repository;

import com.crm.employee.entity.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.crm.employee.repository.EmployeeProfileProjectionTest$SqlCapture"
})
class EmployeeProfileProjectionTest {
    @Autowired EmployeeRepository employees;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory factory;
    public static class SqlCapture implements StatementInspector {
        static final List<String> statements = new ArrayList<>();
        @Override public String inspect(String sql) { statements.add(sql); return sql; }
    }
    Employee save(String code) {
        var employee = new Employee(); employee.setEmployeeCode(code);
        employee.setFirstName("Test"); employee.setLastName("Professional"); employee.setJobTitle("Developer");
        employee.setDepartmentId(7L); employee.setHireDate(LocalDate.of(2026, 1, 15));
        employee.setSalary(new BigDecimal("1234.56")); employee.setAddress("Synthetic private address");
        employee.setDateOfBirth(LocalDate.of(1990, 1, 1)); employee.setEmail("private@example.test");
        employees.saveAndFlush(employee); return employee;
    }
    @Test void selectsExactlySixAllowedColumnsWithoutLoadingAnEntity() {
        var employee = save("PROJECTION-TEST"); entityManager.clear();
        var stats = factory.unwrap(SessionFactory.class).getStatistics(); stats.clear(); SqlCapture.statements.clear();
        var row = employees.findProfileSummaryById(employee.getId()).orElseThrow();
        assertThat(row).isEqualTo(new EmployeeProfileProjection(employee.getId(), "Test", "Professional", "Developer", 7L, LocalDate.of(2026, 1, 15)));
        assertThat(stats.getEntityLoadCount()).isZero();
        assertThat(SqlCapture.statements).hasSize(1);
        String sql = SqlCapture.statements.getFirst().toLowerCase(java.util.Locale.ROOT);
        String selection = sql.substring(sql.indexOf("select ") + 7, sql.indexOf(" from "));
        var columns = java.util.Arrays.stream(selection.split(",")).map(String::trim)
                .map(column -> column.substring(column.lastIndexOf('.') + 1)).toList();
        assertThat(columns).containsExactlyInAnyOrder("id", "first_name", "last_name", "job_title", "department_id", "hire_date");
        assertThat(sql).doesNotContain("salary", "address", "date_of_birth", "email", "phone", "gender", "employee_code");
    }
    @Test void nullableProfessionalFieldsRemainNull() {
        var employee = new Employee(); employee.setEmployeeCode("NULL-PROFILE"); employees.saveAndFlush(employee);
        entityManager.clear();
        assertThat(employees.findProfileSummaryById(employee.getId()).orElseThrow())
                .isEqualTo(new EmployeeProfileProjection(employee.getId(), null, null, null, null, null));
    }
    @Test void nonexistentEmployeeProducesNoProjection() { assertThat(employees.findProfileSummaryById(Long.MAX_VALUE)).isEmpty(); }
    @Test void selectsOnlyTheRequestedEmployee() {
        var first = save("PROFILE-A"); var second = save("PROFILE-B"); entityManager.clear();
        assertThat(employees.findProfileSummaryById(second.getId()).orElseThrow().employeeId()).isEqualTo(second.getId()).isNotEqualTo(first.getId());
    }
}
