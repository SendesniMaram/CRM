package com.crm.hr.repository;

import com.crm.hr.entity.LeaveEmployeeAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface LeaveEmployeeAssociationRepository extends JpaRepository<LeaveEmployeeAssociation, String> {
    boolean existsByEmployeeId(String employeeId);
    Optional<LeaveEmployeeAssociation> findByUserId(Long userId);

    /** Creation must INSERT, never merge an assigned username over a concurrent owner. */
    @Modifying
    @Query(value = "INSERT INTO leave_employee_associations (username, user_id, employee_id) "
            + "VALUES (:username, :userId, :employeeId)", nativeQuery = true)
    void insertNew(@Param("username") String username, @Param("userId") Long userId,
            @Param("employeeId") String employeeId);
}
