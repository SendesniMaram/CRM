package com.crm.identity.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response body returned after a successful token refresh.
 */
public class RefreshTokenResponse {

    private String accessToken;
    private String refreshToken;
    private String type;
    private List<String> roles = new ArrayList<>();
    private long expiration;

    public RefreshTokenResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}

