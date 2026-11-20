package com.crm.hr.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_contracts", indexes = @Index(name = "idx_employee_contracts_employee", columnList = "employee_id,start_date"))
@Data
@NoArgsConstructor
public class EmployeeContract {
    public enum Status { DRAFT, ACTIVE, ENDED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;
    @NotBlank @Column(name = "contract_type", nullable = false, length = 100)
    private String contractType;
    @NotNull @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;
    @Column(name = "parent_contract_id")
    private Long parentContractId;
    @Column(length = 2000)
    private String notes;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
