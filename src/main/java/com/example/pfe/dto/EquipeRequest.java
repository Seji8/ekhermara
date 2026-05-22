package com.example.pfe.dto;

public class EquipeRequest {
    private String nom;
    private Long chefId;

    public EquipeRequest() {}

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public Long getChefId() { return chefId; }
    public void setChefId(Long chefId) { this.chefId = chefId; }
}