package com.cdg.freelance.dto.auth;

public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private UserDto utilisateur;

    public LoginResponse() {}

    public LoginResponse(String token, UserDto utilisateur) {
        this.token = token;
        this.utilisateur = utilisateur;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UserDto getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(UserDto utilisateur) {
        this.utilisateur = utilisateur;
    }
}
