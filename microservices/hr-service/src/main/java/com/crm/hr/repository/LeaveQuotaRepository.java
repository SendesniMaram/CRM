package com.crm.hr.repository;import com.crm.hr.entity.LeaveQuota;import org.springframework.data.jpa.repository.JpaRepository;import java.util.List;
public interface LeaveQuotaRepository extends JpaRepository<LeaveQuota,Long>{List<LeaveQuota> findAllByEmployeeIdOrderByPeriodStartDesc(String employeeId);}
