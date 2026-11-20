package com.crm.hr.repository;
import com.crm.hr.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument,Long> { List<EmployeeDocument> findAllByEmployeeIdOrderByCreatedAtDesc(String employeeId); }
