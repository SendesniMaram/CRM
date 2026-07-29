package com.crm.hr.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrRequest {

    @NotBlank(message = "Employee ID is required")
    @Size(max = 50, message = "Employee ID must be at most 50 characters")
    private String employeeId;

    @Size(max = 100, message = "Contract type must be at most 100 characters")
    private String contractType;

    private LocalDateTime hireDate;

    @Size(max = 255, message = "Job title must be at most 255 characters")
    private String jobTitle;

    @Size(max = 50, message = "Salary grade must be at most 50 characters")
    private String salaryGrade;

    @Size(max = 255, message = "Manager must be at most 255 characters")
    private String manager;

    @Size(max = 255, message = "Work location must be at most 255 characters")
    private String workLocation;

    @Size(max = 50, message = "Employment status must be at most 50 characters")
    private String employmentStatus;
}
