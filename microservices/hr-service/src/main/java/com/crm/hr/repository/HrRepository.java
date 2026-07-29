package com.crm.hr.repository;

import com.crm.hr.entity.HrRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HrRepository extends JpaRepository<HrRecord, Long>, JpaSpecificationExecutor<HrRecord> {

    boolean existsByEmployeeId(String employeeId);
}
