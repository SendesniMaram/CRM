package com.crm.hr.repository;
import com.crm.hr.entity.*;import java.time.LocalDate;import java.util.List;import org.springframework.data.repository.Repository;import org.springframework.data.jpa.repository.Query;import org.springframework.data.repository.query.Param;import org.springframework.data.domain.Pageable;
public interface LeaveBalanceRepository extends Repository<LeaveRequest,Long> {
 interface RequestDays {Long getId();LocalDate getStartDate();LocalDate getEndDate();LeaveRequest.LeaveStatus getStatus();}
 @Query("select l.id as id,l.startDate as startDate,l.endDate as endDate,l.status as status from LeaveRequest l where l.employeeId=:employee and l.startDate<=:to and l.endDate>=:from order by l.startDate,l.id")
 List<RequestDays> requests(@Param("employee") String employee,@Param("from") LocalDate from,@Param("to") LocalDate to,Pageable limit);
 @Query("select q from LeaveQuota q where q.employeeId=:employee and q.periodStart<=:to and q.periodEnd>=:from order by q.periodStart,q.id")
 List<LeaveQuota> quotas(@Param("employee") String employee,@Param("from") LocalDate from,@Param("to") LocalDate to,Pageable limit);
}