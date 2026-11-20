package com.crm.payroll.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payrolls")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Employee ID is required")
    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @NotNull(message = "Base salary is required")
    @Positive(message = "Base salary must be positive")
    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "bonuses", precision = 12, scale = 2)
    private BigDecimal bonuses;

    @Column(name = "deductions", precision = 12, scale = 2)
    private BigDecimal deductions;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name="overtime_hours",precision=8,scale=2)
    private BigDecimal overtimeHours;
    @Column(name="overtime_hourly_rate",precision=12,scale=2)
    private BigDecimal overtimeHourlyRate;
    @Column(name="overtime_amount",precision=12,scale=2)
    private BigDecimal overtimeAmount;


    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;

    @NotBlank(message = "Pay period is required")
    @Column(name = "pay_period", nullable = false)
    private String payPeriod;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "currency")
    private String currency;

    @NotBlank(message = "Status is required")
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name="cancelled",nullable=false)
    private boolean cancelled;
    @Column(name="replacement_payroll_id")
    private Long replacementPayrollId;
    @Column(name="correction_of_id")
    private Long correctionOfId;
    @Column(name="correction_reason",length=500) private String correctionReason;
    @Column(name="corrected_by") private Long correctedBy;
    @Column(name="corrected_at") private LocalDateTime correctedAt;


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

