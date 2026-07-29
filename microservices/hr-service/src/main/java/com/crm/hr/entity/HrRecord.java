package com.crm.hr.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hr_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Employee ID is required")
    @Column(name = "employee_id", nullable = false, unique = true)
    private String employeeId;

    @Column(name = "contract_type")
    private String contractType;

    @Column(name = "hire_date")
    private LocalDateTime hireDate;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "salary_grade")
    private String salaryGrade;

    @Column(name = "manager")
    private String manager;

    @Column(name = "work_location")
    private String workLocation;

    @Column(name = "employment_status")
    private String employmentStatus;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
