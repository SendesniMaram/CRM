package com.crm.hr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Administrative association only for Leave. No cross-database JPA relationship. */
@Entity
@Table(name = "leave_employee_associations")
@Data
@NoArgsConstructor
public class LeaveEmployeeAssociation {
    @Id
    @Column(length = 100)
    private String username;
    @Column(nullable = false, unique = true)
    private Long userId;
    @Column(nullable = false, unique = true, length = 50)
    private String employeeId;
}
