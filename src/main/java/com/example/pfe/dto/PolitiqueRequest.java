// PolitiqueRequest.java
package com.example.pfe.dto;

public class PolitiqueRequest {
    private int annee;
    private int mois;
    private int pourcentageMaxSurSite; // 0-100

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }
    public int getMois() { return mois; }
    public void setMois(int mois) { this.mois = mois; }
    public int getPourcentageMaxSurSite() { return pourcentageMaxSurSite; }
    public void setPourcentageMaxSurSite(int p) { this.pourcentageMaxSurSite = p; }
}