package com.crm.hr.repository;

import com.crm.hr.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    @org.springframework.data.jpa.repository.Query("select l from LeaveRequest l where l.startDate<=:to and l.endDate>=:from and (:employee is null or l.employeeId=:employee) order by l.startDate,l.id")
    List<LeaveRequest> calendar(@org.springframework.data.repository.query.Param("from") java.time.LocalDate from,@org.springframework.data.repository.query.Param("to") java.time.LocalDate to,@org.springframework.data.repository.query.Param("employee") String employee,org.springframework.data.domain.Pageable limit);

    List<LeaveRequest> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);

    List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(java.time.LocalDate end, java.time.LocalDate start);
}
