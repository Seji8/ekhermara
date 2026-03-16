package com.example.pfe.dto;

import com.example.pfe.models.StatutDemande;
import com.example.pfe.models.TypeDemande;
import java.time.LocalDateTime;

public class DemandeTeletravailResponse {
    private Long id;
    private String motif;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private TypeDemande type;
    private StatutDemande statut;
    private String fichierjustificatif;
    private String utilisateurNom;
    private Long utilisateurId;
    private String validateurNom;
    private LocalDateTime dateCreation;
    private int duree;

    // Constructeurs
    public DemandeTeletravailResponse() {}

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public TypeDemande getType() {
        return type;
    }

    public void setType(TypeDemande type) {
        this.type = type;
    }

    public StatutDemande getStatut() {
        return statut;
    }

    public void setStatut(StatutDemande statut) {
        this.statut = statut;
    }

    public String getFichierjustificatif() {
        return fichierjustificatif;
    }

    public void setFichierjustificatif(String fichierjustificatif) {
        this.fichierjustificatif = fichierjustificatif;
    }

    public String getUtilisateurNom() {
        return utilisateurNom;
    }

    public void setUtilisateurNom(String utilisateurNom) {
        this.utilisateurNom = utilisateurNom;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public String getValidateurNom() {
        return validateurNom;
    }

    public void setValidateurNom(String validateurNom) {
        this.validateurNom = validateurNom;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }
}