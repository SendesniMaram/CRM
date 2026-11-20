package com.crm.hr.repository;

import com.crm.hr.entity.EmployeeContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeContractRepository extends JpaRepository<EmployeeContract, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EmployeeContract> {
    List<EmployeeContract> findByEmployeeIdOrderByStartDateDescIdDesc(String employeeId);
    boolean existsByParentContractId(Long parentContractId);
}
