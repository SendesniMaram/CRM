package com.crm.payroll.dto;

import jakarta.validation.constraints.NotNull;

public record PayrollPublicationRequest(@NotNull Boolean published) { }
