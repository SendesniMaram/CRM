package com.crm.hr.repository;
import com.crm.hr.entity.EmployeeEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;import java.util.Optional;
public interface EmployeeEvaluationRepository extends JpaRepository<EmployeeEvaluation,Long>, JpaSpecificationExecutor<EmployeeEvaluation> {
 Page<EmployeeEvaluation> findAllByEmployeeIdAndStatus(String employeeId,EmployeeEvaluation.Status status,Pageable pageable);
 Optional<EmployeeEvaluation> findByIdAndEmployeeIdAndStatus(Long id,String employeeId,EmployeeEvaluation.Status status);
}
