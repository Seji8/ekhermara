package com.example.pfe.models;

import jakarta.persistence.*;
import java.util.Map;

@Entity
@Table(name = "equipe")
public class Equipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public float getPourcentageSurSite() {
        return 0;
    }

    public Map<String, Object> getStatistiques() {
        return Map.of();
    }
}
