package com.crm.hr.dto;

import com.crm.hr.entity.EmployeeContract;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeContractRequest(
        @NotBlank @Size(max = 50) String employeeId,
        @NotBlank @Size(max = 100) String contractType,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull EmployeeContract.Status status,
        Long parentContractId,
        @Size(max = 2000) String notes) {}
