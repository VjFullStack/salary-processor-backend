package com.salaryprocessor.model;

import lombok.Data;

@Data
public class JwtResponse {
    private String token;
    
    // Explicit constructors
    public JwtResponse() {
    }
    
    public JwtResponse(String token) {
        this.token = token;
    }
    
    // Explicit getters and setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}
