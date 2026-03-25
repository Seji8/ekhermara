package com.example.pfe.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "equipe")
public class Equipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @OneToOne
    @JoinColumn(name = "chef_id")
    @JsonIgnoreProperties({"equipe", "password", "membres"})
    private User chef;  // Le chef de l'équipe

    @OneToMany(mappedBy = "equipe")
    @JsonIgnoreProperties("equipe")
    private List<User> membres = new ArrayList<>();

    // Constructeurs
    public Equipe() {}

    public Equipe(String nom) {
        this.nom = nom;
    }

    // Getters et Setters
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

    public User getChef() {
        return chef;
    }

    public void setChef(User chef) {
        this.chef = chef;
    }

    public List<User> getMembres() {
        return membres;
    }

    public void setMembres(List<User> membres) {
        this.membres = membres;
    }

    // Méthodes existantes
    public float getPourcentageSurSite() {
        return 0;
    }

    public Map<String, Object> getStatistiques() {
        return Map.of();
    }
}