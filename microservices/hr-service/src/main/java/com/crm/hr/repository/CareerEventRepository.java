package com.crm.hr.repository;
import com.crm.hr.entity.CareerEvent; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface CareerEventRepository extends JpaRepository<CareerEvent,Long>{ List<CareerEvent> findAllByEmployeeIdOrderByEffectiveDateDescIdDesc(String employeeId); }
