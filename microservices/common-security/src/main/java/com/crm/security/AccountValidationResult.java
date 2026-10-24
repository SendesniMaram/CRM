package com.crm.security;

public record AccountValidationResult(Status status, JwtService.AccessTokenClaims claims) {
    public enum Status { VALID, INVALID, UNAVAILABLE }
    public AccountValidationResult {
        if (status == null || (status == Status.VALID) != (claims != null)) {
            throw new IllegalArgumentException("Inconsistent validation result");
        }
    }
    public static AccountValidationResult invalid() { return new AccountValidationResult(Status.INVALID, null); }
    public static AccountValidationResult unavailable() { return new AccountValidationResult(Status.UNAVAILABLE, null); }
}
