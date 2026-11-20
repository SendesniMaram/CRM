package com.crm.invoice.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;

/** Protects service calls as well as HTTP requests, including quote conversion. */
public final class FinancialValidation {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final BigDecimal MAX = new BigDecimal("9999999999.99");
    private FinancialValidation() {}
    public static void request(Object request) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        var errors = VALIDATOR.validate(request);
        if (!errors.isEmpty()) throw new IllegalArgumentException("Invalid financial request: " +
                errors.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted()
                        .collect(java.util.stream.Collectors.joining(", ")));
    }
    public static void calculated(BigDecimal amount) {
        if (amount.signum() < 0 || amount.compareTo(MAX) > 0 || amount.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("Calculated amount must be non-negative and fit DECIMAL(12,2)");
    }
}
