package com.crm.hr.repository;

import com.crm.hr.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    List<LeaveRequest> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);

    List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(java.time.LocalDate end, java.time.LocalDate start);
}
