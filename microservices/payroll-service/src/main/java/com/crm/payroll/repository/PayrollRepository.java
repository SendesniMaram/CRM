package com.crm.payroll.repository;

import com.crm.payroll.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long>, JpaSpecificationExecutor<Payroll> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payroll p where p.id=:id")
    java.util.Optional<Payroll> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    org.springframework.data.domain.Page<Payroll> findByPayPeriod(String period,org.springframework.data.domain.Pageable pageable);
    boolean existsByEmployeeId(String employeeId);
}

