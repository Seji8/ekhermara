package com.example.pfe.dto;

import com.example.pfe.models.StatutDemande;
import com.example.pfe.models.TypeDemande;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private String utilisateurEmail;
    private String utilisateurRole;
    private String utilisateurEquipe;
    private String validateurNom;
    private LocalDateTime dateCreation;
    private int duree;
    private String processInstanceId;

    // Motif de rejet visible par l'employe
    private String motifRejet;

    // Champs pour le suivi
    private int etapeValidation;
    private List<ValidationResponse> historiqueValidations;

    // PAS de champ prochainValidateur — methode manuelle uniquement
    // Evite le conflit Lombok @Data qui generait un getter concurrent
    // et desorganisait la serialisation Jackson (causait motifRejet=null)
    @JsonProperty("prochainValidateur")
    public String getProchainValidateur() {
        if (statut == StatutDemande.APPROVED) return "Validation terminee";
        if (statut == StatutDemande.REJECTED) return "Demande rejetee";

        boolean chefApproved = historiqueValidations != null && historiqueValidations.stream()
                .anyMatch(v -> v.getValidateurRole() != null
                        && v.getValidateurRole().equals("CHEF_EQUIPE")
                        && v.getStatut().equals("APPROVED"));
        boolean adminApproved = historiqueValidations != null && historiqueValidations.stream()
                .anyMatch(v -> v.getValidateurRole() != null
                        && v.getValidateurRole().equals("ADMIN")
                        && v.getStatut().equals("APPROVED"));

        if (!chefApproved && !adminApproved) return "Chef d'equipe ou Administrateur";
        if (!chefApproved) return "Chef d'equipe";
        if (!adminApproved) return "Administrateur";
        return "Validation terminee";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResponse {
        private Long id;
        private String validateurNom;
        private String validateurRole;
        private String statut;
        private String commentaire;
        private LocalDateTime dateValidation;
        private int etapeValidation;
    }
}