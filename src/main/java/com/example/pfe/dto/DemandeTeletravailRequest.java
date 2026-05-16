package com.example.pfe.dto;

import com.example.pfe.models.TypeDemande;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

public class DemandeTeletravailRequest {
    private String motif;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private TypeDemande type;
    private MultipartFile fichier;

    // Getters et Setters
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

    public MultipartFile getFichier() {
        return fichier;
    }

    public void setFichier(MultipartFile fichier) {
        this.fichier = fichier;
    }
}