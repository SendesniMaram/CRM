package com.crm.identity.exception;

/**
 * Business exception thrown when provided login credentials are invalid.
 */
public class InvalidCredentialsException extends IdentityServiceException {

    public InvalidCredentialsException() {
        super();
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}

