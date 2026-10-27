package com.crm.hr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="employee_evaluations") @Data @NoArgsConstructor
public class EmployeeEvaluation {
    public enum Status { DRAFT, FINALIZED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="employee_id",nullable=false,length=50) private String employeeId;
    @Column(name="period_start",nullable=false) private LocalDate periodStart;
    @Column(name="period_end",nullable=false) private LocalDate periodEnd;
    @Column(columnDefinition="TEXT") private String criteria;
    @Column(columnDefinition="TEXT") private String comments;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.DRAFT;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    @PrePersist void create(){createdAt=updatedAt=LocalDateTime.now();}
    @PreUpdate void update(){updatedAt=LocalDateTime.now();}
}
