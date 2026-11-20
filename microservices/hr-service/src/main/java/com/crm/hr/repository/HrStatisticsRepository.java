package com.crm.hr.repository;
import com.crm.hr.entity.LeaveRequest;import java.time.*;import java.util.List;import org.springframework.data.repository.Repository;import org.springframework.data.jpa.repository.Query;import org.springframework.data.repository.query.Param;
/** Six aggregate queries; no entity table materialisation or employee lookup. */
public interface HrStatisticsRepository extends Repository<LeaveRequest,Long> {
 interface StatusCount {String getStatus();long getTotal();}
 @Query(value="select status as status,count(*) as total from leave_requests where start_date<=:to and end_date>=:from group by status",nativeQuery=true)
 List<StatusCount> leaves(@Param("from") LocalDate from,@Param("to") LocalDate to);
 @Query(value="select status as status,count(*) as total from trainings where start_date<=:to and end_date>=:from group by status",nativeQuery=true)
 List<StatusCount> trainings(@Param("from") LocalDate from,@Param("to") LocalDate to);
 @Query(value="select status as status,count(*) as total from employee_evaluations where period_start<=:to and period_end>=:from group by status",nativeQuery=true)
 List<StatusCount> evaluations(@Param("from") LocalDate from,@Param("to") LocalDate to);
 @Query(value="select status as status,count(*) as total from job_offers where created_at>=:from and created_at<:until group by status",nativeQuery=true)
 List<StatusCount> offers(@Param("from") LocalDateTime from,@Param("until") LocalDateTime until);
 @Query(value="select status as status,count(*) as total from job_applications where created_at>=:from and created_at<:until group by status",nativeQuery=true)
 List<StatusCount> applications(@Param("from") LocalDateTime from,@Param("until") LocalDateTime until);
 @Query(value="""
 WITH RECURSIVE days(d) AS (SELECT CAST(:from AS DATE) UNION ALL SELECT DATE_ADD(d,INTERVAL 1 DAY) FROM days WHERE d<:to)
 SELECT COUNT(*) FROM (SELECT DISTINCT l.employee_id,d.d FROM leave_requests l JOIN days d ON d.d BETWEEN l.start_date AND l.end_date WHERE l.status='APPROVED') approved_slots
 """,nativeQuery=true)
 long approvedEmployeeDays(@Param("from") LocalDate from,@Param("to") LocalDate to);
}