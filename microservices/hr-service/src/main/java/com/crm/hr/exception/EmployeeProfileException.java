package com.crm.hr.exception;

/** Fixed classifications only; never retain transport diagnostics or credentials as causes. */
public final class EmployeeProfileException extends RuntimeException {
    private final int status;
    private final String code;
    public EmployeeProfileException(int status, String code) { super("Employee profile request could not be completed"); this.status = status; this.code = code; }
    public int status() { return status; }
    public String code() { return code; }
}
