package com.crm.hr.repository;

import com.crm.hr.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {

    List<Attendance> findByEmployeeId(String employeeId);

    Optional<Attendance> findByEmployeeIdAndDate(String employeeId, LocalDate date);

    List<Attendance> findByDate(LocalDate date);
}
