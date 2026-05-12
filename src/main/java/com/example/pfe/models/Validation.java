package com.example.pfe.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "validations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Validation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String validateurNom;
    private String validateurRole;
    @ManyToOne
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeTeletravail demande;

    @ManyToOne
    @JoinColumn(name = "validateur_id", nullable = false)
    private User validateur;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;

    private String commentaire;

    private LocalDateTime dateValidation;

    private int etape;

    @PrePersist
    protected void onCreate() {
        dateValidation = LocalDateTime.now();
    }
}