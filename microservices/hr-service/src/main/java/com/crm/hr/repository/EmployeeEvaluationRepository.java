package com.crm.hr.repository;
import com.crm.hr.entity.EmployeeEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface EmployeeEvaluationRepository extends JpaRepository<EmployeeEvaluation,Long>, JpaSpecificationExecutor<EmployeeEvaluation> {}
