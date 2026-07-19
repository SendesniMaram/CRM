package com.crm.identity.service;

import com.crm.identity.dto.LoginRequest;
import com.crm.identity.dto.LoginResponse;
import com.crm.identity.dto.RegisterRequest;

/**
 * Future contract for authentication service.
 */
public interface IAuthenticationService {

    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);
}



