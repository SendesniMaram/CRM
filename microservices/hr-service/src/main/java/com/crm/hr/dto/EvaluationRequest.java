package com.crm.hr.dto;
import com.crm.hr.entity.EmployeeEvaluation;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record EvaluationRequest(@NotBlank @Size(max=50) String employeeId,@NotNull LocalDate periodStart,@NotNull LocalDate periodEnd,@Size(max=10000) String criteria,@Size(max=10000) String comments,@NotNull EmployeeEvaluation.Status status) {}
