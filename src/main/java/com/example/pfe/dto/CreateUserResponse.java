package com.example.pfe.dto;

import com.example.pfe.models.Role;
import com.example.pfe.models.Equipe;
import com.example.pfe.models.User;

public class CreateUserResponse {
    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
    private String ville;
    private String matricule;
    private Role role;
    private Long equipeId;
    private String equipeNom;

    // Constructeurs
    public CreateUserResponse() {}

    public CreateUserResponse(User user) {
        this.id = user.getId();
        this.nom = user.getNom();
        this.email = user.getEmail();
        this.telephone = user.getTelephone();
        this.adresse = user.getAdresse();
        this.ville = user.getVille();
        this.matricule = user.getMatricule();
        this.role = user.getRole();

        if (user.getEquipe() != null) {
            this.equipeId = user.getEquipe().getId();
            this.equipeNom = user.getEquipe().getNom();
        }
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Long getEquipeId() { return equipeId; }
    public void setEquipeId(Long equipeId) { this.equipeId = equipeId; }

    public String getEquipeNom() { return equipeNom; }
    public void setEquipeNom(String equipeNom) { this.equipeNom = equipeNom; }
}