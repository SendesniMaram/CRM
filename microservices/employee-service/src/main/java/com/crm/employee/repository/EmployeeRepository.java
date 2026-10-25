package com.crm.employee.repository;

import com.crm.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    @Query("""
            select new com.crm.employee.repository.EmployeeProfileProjection(
                e.id, e.firstName, e.lastName, e.jobTitle, e.departmentId, e.hireDate)
            from Employee e where e.id = :employeeId
            """)
    Optional<EmployeeProfileProjection> findProfileSummaryById(@Param("employeeId") Long employeeId);
}

