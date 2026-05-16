package com.example.pfe.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demandes_teletravail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeTeletravail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String processInstanceId;
    private String fichierjustificatif;
    private String motif;
    @Column(name = "motif_rejet", length = 1000)
    private String motifRejet;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;

    @ManyToOne
    @JoinColumn(name = "validateur_id")
    private User validateur;

    @Enumerated(EnumType.STRING)
    private TypeDemande type;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Validation> validations = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)

    @JoinColumn(name = "demande_id")
    private List<Validation> historiqueValidations = new ArrayList<>();


    private int etapeValidation = 1;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    public int getDuree() {
        if (dateDebut != null && dateFin != null) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin);
        }
        return 0;
    }

    public Validation getLastValidation() {
        if (validations.isEmpty()) {
            return null;
        }
        return validations.get(validations.size() - 1);
    }

    public String getProchainValidateur() {
        switch (etapeValidation) {
            case 1: return "Chef d'équipe";
            case 2: return "Administrateur";  // ← Change ici aussi
            default: return "Validation terminée";
        }
    }
}