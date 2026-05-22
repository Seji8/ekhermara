// PolitiqueResponse.java
package com.example.pfe.dto;

public class PolitiqueResponse {
    private Long id;
    private int annee;
    private int mois;
    private int pourcentageMaxSurSite;
    private String nomEquipe;
    // % télétravail déjà consommé ce mois
    private double pourcentageActuel;
    private int membresEnTeletravail;
    private int totalMembres;

    // Getters & Setters (tous)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getAnnee() { return annee; }
    public void setAnnee(int a) { this.annee = a; }
    public int getMois() { return mois; }
    public void setMois(int m) { this.mois = m; }
    public int getPourcentageMaxSurSite() { return pourcentageMaxSurSite; }
    public void setPourcentageMaxSurSite(int p) { this.pourcentageMaxSurSite = p; }
    public String getNomEquipe() { return nomEquipe; }
    public void setNomEquipe(String n) { this.nomEquipe = n; }
    public double getPourcentageActuel() { return pourcentageActuel; }
    public void setPourcentageActuel(double p) { this.pourcentageActuel = p; }
    public int getMembresEnTeletravail() { return membresEnTeletravail; }
    public void setMembresEnTeletravail(int m) { this.membresEnTeletravail = m; }
    public int getTotalMembres() { return totalMembres; }
    public void setTotalMembres(int t) { this.totalMembres = t; }
}