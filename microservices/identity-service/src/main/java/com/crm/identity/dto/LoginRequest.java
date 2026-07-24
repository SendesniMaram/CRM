package com.crm.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Authentication login request.
 */
public class LoginRequest {

    @Size(max = 100, message = "username must not exceed 100 characters")
    private String username;

    @Size(max = 100, message = "email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "password is required")
    @Size(max = 100, message = "password must not exceed 100 characters")
    private String password;

    public LoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}


