package com.example.pfe.dto;

import com.example.pfe.models.Equipe;

public class EquipeResponse {
    private Long id;
    private String nom;
    private Long chefId;
    private String chefNom;
    private int nombreMembres;

    public EquipeResponse() {}

    public EquipeResponse(Equipe equipe) {
        this.id = equipe.getId();
        this.nom = equipe.getNom();
        if (equipe.getChef() != null) {
            this.chefId = equipe.getChef().getId();
            this.chefNom = equipe.getChef().getNom();
        }
        this.nombreMembres = equipe.getMembres() != null ? equipe.getMembres().size() : 0;

        // Log pour debug
        System.out.println("EquipeResponse créée: " + this.nom +
                ", chefId: " + this.chefId +
                ", chefNom: " + this.chefNom);
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public Long getChefId() { return chefId; }
    public void setChefId(Long chefId) { this.chefId = chefId; }

    public String getChefNom() { return chefNom; }
    public void setChefNom(String chefNom) { this.chefNom = chefNom; }

    public int getNombreMembres() { return nombreMembres; }
    public void setNombreMembres(int nombreMembres) { this.nombreMembres = nombreMembres; }
}