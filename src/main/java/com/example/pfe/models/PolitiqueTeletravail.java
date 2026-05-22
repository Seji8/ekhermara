package com.example.pfe.models;

import jakarta.persistence.*;
import java.time.YearMonth;

@Entity
@Table(name = "politique_teletravail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"equipe_id", "annee", "mois"}))
public class PolitiqueTeletravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id", nullable = false)
    private Equipe equipe;

    @Column(nullable = false)
    private int annee;

    @Column(nullable = false)
    private int mois;

    @Column(nullable = false)
    private int pourcentageMaxSurSite;

    // Getters & Setters
    public Long getId() { return id; }
    public Equipe getEquipe() { return equipe; }
    public void setEquipe(Equipe equipe) { this.equipe = equipe; }
    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }
    public int getMois() { return mois; }
    public void setMois(int mois) { this.mois = mois; }
    public int getPourcentageMaxSurSite() { return pourcentageMaxSurSite; }
    public void setPourcentageMaxSurSite(int p) { this.pourcentageMaxSurSite = p; }
}