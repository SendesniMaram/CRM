package com.crm.fees.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeRequest {

    @NotBlank(message = "Employee ID is required")
    @Size(max = 50, message = "Employee ID must be at most 50 characters")
    private String employeeId;

    @Size(max = 50, message = "Customer ID must be at most 50 characters")
    private String customerId;

    @NotBlank(message = "Service name is required")
    @Size(max = 255, message = "Service name must be at most 255 characters")
    private String serviceName;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotNull(message = "Hourly rate is required")
    @Positive(message = "Hourly rate must be positive")
    private BigDecimal hourlyRate;

    @NotNull(message = "Worked hours is required")
    @Positive(message = "Worked hours must be positive")
    private BigDecimal workedHours;

    @Size(max = 50, message = "Invoice status must be at most 50 characters")
    private String invoiceStatus;

    @Size(max = 50, message = "Payment status must be at most 50 characters")
    private String paymentStatus;

    private LocalDate workDate;
}
