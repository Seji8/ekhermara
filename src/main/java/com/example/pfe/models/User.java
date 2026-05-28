package com.example.pfe.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;


    // Informations personnelles
    @Column(name = "telephone")
    private String telephone;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "ville")
    private String ville;

    // Informations professionnelles
    @Column(name = "matricule", unique = true)
    private String matricule; // Numéro d'employé

    @ManyToOne
    @JoinColumn(name = "equipe_id")
    @JsonIgnore
    private Equipe equipe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;


    // Méthodes de vérification
    public boolean estChef() {
        return this.role == Role.CHEF_EQUIPE;
    }

    public boolean estRH() {
        return this.role == Role.RH;
    }

    public boolean estAdmin() {
        return this.role == Role.ADMIN;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public Equipe getEquipe() {
        return equipe;
    }

    public void setEquipe(Equipe equipe) {
        this.equipe = equipe;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

}