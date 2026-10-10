package com.crm.hr.repository;

import com.crm.hr.entity.LeaveEmployeeAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LeaveEmployeeAssociationRepository extends JpaRepository<LeaveEmployeeAssociation, String> {
    boolean existsByEmployeeId(String employeeId);
    Optional<LeaveEmployeeAssociation> findByUserId(Long userId);
}
