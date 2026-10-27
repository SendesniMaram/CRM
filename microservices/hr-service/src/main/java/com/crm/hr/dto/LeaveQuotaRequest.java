package com.crm.hr.dto;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.LocalDate;
public record LeaveQuotaRequest(@NotBlank @Pattern(regexp="[1-9][0-9]{0,18}")String employeeId,@NotNull LocalDate periodStart,@NotNull LocalDate periodEnd,@NotBlank @Size(max=50)String leaveType,@NotNull @DecimalMin("0.00") @Digits(integer=5,fraction=2)BigDecimal grantedDays){}
