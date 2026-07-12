package com.crm.service;

import com.crm.dto.LoginRequest;
import com.crm.dto.LoginResponse;
import com.crm.dto.RegisterRequest;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
