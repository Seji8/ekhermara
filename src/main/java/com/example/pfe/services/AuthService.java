package com.example.pfe.services;

import com.example.pfe.dto.LoginRequest;

import java.util.Map;

public interface AuthService {

    Map<String, Object> login(LoginRequest request);

}