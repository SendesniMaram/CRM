package com.crm.identity.service;

import com.crm.identity.dto.LoginRequest;
import com.crm.identity.dto.LoginResponse;
import com.crm.identity.dto.RefreshTokenRequest;
import com.crm.identity.dto.RefreshTokenResponse;
import com.crm.identity.dto.RegisterRequest;

/**
 * Contract for authentication service.
 */
public interface IAuthenticationService {

    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);
}



