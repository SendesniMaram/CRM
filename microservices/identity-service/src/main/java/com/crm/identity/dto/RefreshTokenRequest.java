package com.crm.identity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for refreshing a JWT access token.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken is required")
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

