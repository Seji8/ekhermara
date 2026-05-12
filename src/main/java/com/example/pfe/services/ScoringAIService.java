package com.example.pfe.services;

import com.example.pfe.models.DemandeTeletravail;
import com.example.pfe.models.User;
import com.example.pfe.models.Equipe;
import com.example.pfe.models.StatutDemande;
import com.example.pfe.models.TypeDemande;
import com.example.pfe.repository.DemandeTeletravailRepository;
import com.example.pfe.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoringAIService {

    private final DemandeTeletravailRepository demandeRepository;
    private final UserRepository userRepository;

    public ScoringAIService(DemandeTeletravailRepository demandeRepository,
                            UserRepository userRepository) {
        this.demandeRepository = demandeRepository;
        this.userRepository = userRepository;
    }

    // ✅ Helper: calculer la durée réelle en jours entre dateDebut et dateFin
    private long calculerDuree(DemandeTeletravail demande) {
        if (demande.getDateDebut() == null || demande.getDateFin() == null) return 0;
        long duree = ChronoUnit.DAYS.between(demande.getDateDebut(), demande.getDateFin());
        return Math.max(0, duree);
    }

    /**
     * Score complet avec analyse IA
     */
    public Map<String, Object> calculerScoreAvecIA(Long demandeId) {
        DemandeTeletravail demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        User utilisateur = demande.getUtilisateur();

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("conflit", calculerScoreConflit(demande));
        // ✅ Fix: passer demandeId pour l'exclure de l'historique
        result.put("historique", calculerScoreHistorique(utilisateur, demandeId));
        result.put("fiabilite", calculerScoreFiabilite(utilisateur));
        result.put("demandeCourante", calculerScoreDemandeCourante(demande));

        int scoreGlobal = calculerScoreGlobal(result);
        result.put("scoreGlobal", scoreGlobal);
        result.put("niveauRisque", getNiveauRisque(scoreGlobal));
        result.put("recommandation", getRecommandation(scoreGlobal, result));
        result.put("couleur", getCouleurRisque(scoreGlobal));

        return result;
    }

    /**
     * Score de conflit
     */
    public Map<String, Object> calculerScoreConflit(DemandeTeletravail demande) {
        User utilisateur = demande.getUtilisateur();
        Equipe equipe = utilisateur.getEquipe();

        Map<String, Object> conflit = new LinkedHashMap<>();

        if (equipe == null) {
            conflit.put("score", 50);
            conflit.put("niveau", "NON_APPLICABLE");
            conflit.put("chevauchements", 0);
            conflit.put("tailleEquipe", 0);
            conflit.put("description", "⚠️ Utilisateur sans équipe");
            return conflit;
        }

        List<User> membres = userRepository.findByEquipeId(equipe.getId()).stream()
                .filter(m -> !m.getId().equals(utilisateur.getId()))
                .collect(Collectors.toList());

        int tailleEquipe = membres.size();
        if (tailleEquipe == 0) {
            conflit.put("score", 100);
            conflit.put("niveau", "FAIBLE");
            conflit.put("chevauchements", 0);
            conflit.put("tailleEquipe", 0);
            conflit.put("description", "✅ Seul membre de l'équipe");
            return conflit;
        }

        if (demande.getDateDebut() == null || demande.getDateFin() == null) {
            conflit.put("score", 50);
            conflit.put("niveau", "INCONNU");
            conflit.put("chevauchements", 0);
            conflit.put("tailleEquipe", tailleEquipe);
            conflit.put("description", "⚠️ Dates non spécifiées");
            return conflit;
        }

        List<DemandeTeletravail> demandesChevauchement = new ArrayList<>();

        for (User membre : membres) {
            List<DemandeTeletravail> demandesMembre = demandeRepository.findByUtilisateurId(membre.getId());
            for (DemandeTeletravail d : demandesMembre) {
                if (StatutDemande.APPROVED.equals(d.getStatut())
                        && d.getDateDebut() != null && d.getDateFin() != null
                        && !d.getDateDebut().isAfter(demande.getDateFin())
                        && !d.getDateFin().isBefore(demande.getDateDebut())) {
                    demandesChevauchement.add(d);
                }
            }
        }

        long chevauchements = demandesChevauchement.stream()
                .map(d -> d.getUtilisateur().getId())
                .distinct()
                .count();

        int scoreConflit = (int) Math.round((chevauchements * 100.0) / tailleEquipe);
        int scoreFinal = 100 - scoreConflit;

        conflit.put("score", scoreFinal);
        conflit.put("niveau", scoreConflit >= 60 ? "CRITIQUE" : scoreConflit >= 30 ? "MOYEN" : "FAIBLE");
        conflit.put("chevauchements", chevauchements);
        conflit.put("tailleEquipe", tailleEquipe);
        conflit.put("description", getConflitDescription(scoreConflit));

        return conflit;
    }

    private String getConflitDescription(int scoreConflit) {
        if (scoreConflit >= 60) return "⚠️ Conflit élevé - plusieurs membres déjà en TT";
        if (scoreConflit >= 30) return "🟡 Conflit modéré - certains membres en TT";
        return "✅ Bonne disponibilité de l'équipe";
    }

    /**
     * Score basé sur l'historique mensuel
     * ✅ Fix: exclure la demande courante de l'historique
     */
    private Map<String, Object> calculerScoreHistorique(User utilisateur, Long demandeIdCourante) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sixMoisAgo = now.minusMonths(6);

        List<DemandeTeletravail> toutesDemandes = demandeRepository.findByUtilisateurId(utilisateur.getId());
        List<DemandeTeletravail> demandes = toutesDemandes.stream()
                // ✅ Exclure la demande courante
                .filter(d -> !d.getId().equals(demandeIdCourante))
                // ✅ Seulement les 6 derniers mois
                .filter(d -> d.getDateCreation() != null && d.getDateCreation().isAfter(sixMoisAgo))
                .collect(Collectors.toList());

        Map<String, Object> historique = new LinkedHashMap<>();
        Map<String, Object> monthsMap = new LinkedHashMap<>();
        Map<String, Integer> moisDemandes = new LinkedHashMap<>();

        for (DemandeTeletravail d : demandes) {
            String monthKey = d.getDateCreation()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            moisDemandes.merge(monthKey, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : moisDemandes.entrySet()) {
            String yearMonth = entry.getKey();
            Map<String, Object> monthStats = new LinkedHashMap<>();
            monthStats.put("total", entry.getValue());

            int occasionnel = 0, regulier = 0, complet = 0;
            for (DemandeTeletravail d : demandes) {
                String dMonth = d.getDateCreation()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                if (dMonth.equals(yearMonth)) {
                    if (d.getType() == TypeDemande.OCCASIONAL) occasionnel++;
                    else if (d.getType() == TypeDemande.REGULAR) regulier++;
                    else if (d.getType() == TypeDemande.FULL) complet++;
                }
            }
            monthStats.put("occasionnel", occasionnel);
            monthStats.put("regulier", regulier);
            monthStats.put("complet", complet);

            monthsMap.put(getMonthName(yearMonth), monthStats);
        }

        int totalDemandes = demandes.size();
        double moyParMois = moisDemandes.isEmpty() ? 0
                : totalDemandes / (double) moisDemandes.size();
        long refusCount = demandes.stream()
                .filter(d -> d.getStatut() == StatutDemande.REJECTED).count();
        double tauxRefus = totalDemandes == 0 ? 0
                : (refusCount * 100.0) / totalDemandes;

        int scoreHistorique = 100;
        if (totalDemandes > 10) scoreHistorique -= 30;
        else if (totalDemandes > 6) scoreHistorique -= 15;
        else if (totalDemandes > 3) scoreHistorique -= 5;

        if (tauxRefus > 30) scoreHistorique -= 25;
        else if (tauxRefus > 15) scoreHistorique -= 10;

        historique.put("score", Math.max(0, scoreHistorique));
        historique.put("totalDemandes", totalDemandes);
        historique.put("moyenneParMois", Math.round(moyParMois * 10) / 10.0);
        historique.put("tauxRefus", Math.round(tauxRefus * 10) / 10.0);
        historique.put("analyseMensuelle", monthsMap);
        historique.put("niveau", scoreHistorique >= 80 ? "EXCELLENT"
                : scoreHistorique >= 60 ? "BON" : "MOYEN");

        return historique;
    }

    private String getMonthName(String yearMonth) {
        String[] parts = yearMonth.split("-");
        int month = Integer.parseInt(parts[1]);
        String[] months = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
                "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"};
        return months[month - 1] + " " + parts[0];
    }

    /**
     * Score de fiabilité
     */
    private Map<String, Object> calculerScoreFiabilite(User utilisateur) {
        Map<String, Object> fiabilite = new LinkedHashMap<>();

        List<DemandeTeletravail> toutesDemandes = demandeRepository.findByUtilisateurId(utilisateur.getId());
        long totalApprouvees = toutesDemandes.stream()
                .filter(d -> d.getStatut() == StatutDemande.APPROVED)
                .count();
        long totalRejettees = toutesDemandes.stream()
                .filter(d -> d.getStatut() == StatutDemande.REJECTED)
                .count();
        long totalTraitees = totalApprouvees + totalRejettees;

        // ✅ Calculer le vrai taux de respect à partir des données réelles
        int tauxRespect = totalTraitees == 0 ? 90
                : (int) Math.round((totalApprouvees * 100.0) / totalTraitees);

        fiabilite.put("totalApprouvees", (int) totalApprouvees);
        fiabilite.put("tauxRespect", tauxRespect);
        fiabilite.put("score", tauxRespect);
        fiabilite.put("niveau", tauxRespect >= 80 ? "HAUTE" : tauxRespect >= 60 ? "MOYENNE" : "FAIBLE");

        return fiabilite;
    }

    /**
     * Score de la demande courante
     * ✅ Fix: calculer la durée réelle au lieu d'utiliser getDuree()
     */
    private Map<String, Object> calculerScoreDemandeCourante(DemandeTeletravail demande) {
        Map<String, Object> courante = new LinkedHashMap<>();

        int score = 100;
        List<String> pointsPositifs = new ArrayList<>();
        List<String> pointsAttention = new ArrayList<>();

        // ✅ Fix: calculer la vraie durée
        long duree = calculerDuree(demande);

        if (duree > 10) {
            score -= 25;
            pointsAttention.add("Longue durée: " + duree + " jours");
        } else if (duree > 5) {
            score -= 10;
            pointsAttention.add("Durée moyenne: " + duree + " jours");
        } else if (duree == 0) {
            score -= 5;
            pointsAttention.add("Durée nulle ou non calculable");
        } else {
            pointsPositifs.add("Durée courte: " + duree + " jour(s)");
        }

        if (demande.getType() == TypeDemande.OCCASIONAL) {
            pointsPositifs.add("Télétravail occasionnel");
        } else if (demande.getType() == TypeDemande.REGULAR) {
            pointsPositifs.add("Télétravail régulier");
        } else {
            pointsAttention.add("Télétravail complet");
        }

        if (demande.getMotif() != null && !demande.getMotif().isEmpty()) {
            pointsPositifs.add("Motif fourni");
        } else {
            score -= 15;
            pointsAttention.add("Aucun motif fourni");
        }

        courante.put("score", Math.max(0, score));
        courante.put("duree", duree);
        courante.put("type", demande.getType() != null ? demande.getType().toString() : "INCONNU");
        courante.put("pointsPositifs", pointsPositifs);
        courante.put("pointsAttention", pointsAttention);

        return courante;
    }

    /**
     * Score global pondéré
     */
    private int calculerScoreGlobal(Map<String, Object> scores) {
        Map<String, Object> conflit = (Map<String, Object>) scores.get("conflit");
        Map<String, Object> historique = (Map<String, Object>) scores.get("historique");
        Map<String, Object> fiabilite = (Map<String, Object>) scores.get("fiabilite");
        Map<String, Object> demandeCourante = (Map<String, Object>) scores.get("demandeCourante");

        int conflitScore = (int) conflit.getOrDefault("score", 0);
        int historiqueScore = (int) historique.getOrDefault("score", 0);
        int fiabiliteScore = (int) fiabilite.getOrDefault("score", 0);
        int demandeScore = (int) demandeCourante.getOrDefault("score", 0);

        double scorePondere = (conflitScore * 0.40)
                + (historiqueScore * 0.20)
                + (fiabiliteScore * 0.15)
                + (demandeScore * 0.25);

        return (int) Math.round(scorePondere);
    }

    private String getNiveauRisque(int score) {
        if (score >= 80) return "FAIBLE";
        if (score >= 65) return "MODÉRÉ";
        if (score >= 45) return "ÉLEVÉ";
        return "CRITIQUE";
    }

    private String getCouleurRisque(int score) {
        if (score >= 80) return "#28a745";
        if (score >= 65) return "#ffc107";
        if (score >= 45) return "#fd7e14";
        return "#dc3545";
    }

    private String getRecommandation(int score, Map<String, Object> details) {
        if (score >= 80) return "APPROUVER - Score excellent";
        if (score >= 65) return "APPROUVER SOUS CONDITIONS - Vérifier points d'attention";
        if (score >= 45) {
            Map<String, Object> conflit = (Map<String, Object>) details.get("conflit");
            int conflitScore = (int) conflit.getOrDefault("score", 0);
            if (conflitScore < 40) return "EXAMEN APPROFONDI - Évaluer manuellement";
            return "DÉCONSEILLER - Risques identifiés";
        }
        return "REFUSER - Risque trop élevé";
    }
}