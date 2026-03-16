package com.example.pfe.dto;

import com.example.pfe.models.Role;

public class CreateUserRequest {
    private String nom;
    private String email;
    private Role role;



    public CreateUserRequest() {}

    public CreateUserRequest(String nom, String email, Role role) {
        this.nom = nom;
        this.email = email;
        this.role = role;
    }

    // Getters and Setters
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}