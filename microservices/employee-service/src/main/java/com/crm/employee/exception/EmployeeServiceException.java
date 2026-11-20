package com.crm.employee.exception;

public class EmployeeServiceException extends RuntimeException {

    public EmployeeServiceException() {
        super();
    }

    public EmployeeServiceException(String message) {
        super(message);
    }

    public EmployeeServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
