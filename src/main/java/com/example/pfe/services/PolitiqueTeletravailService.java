package com.example.pfe.services;

import com.example.pfe.dto.PolitiqueRequest;
import com.example.pfe.dto.PolitiqueResponse;
import com.example.pfe.models.*;
import com.example.pfe.repository.DemandeTeletravailRepository;
import com.example.pfe.repository.PolitiqueTeletravailRepository;
import com.example.pfe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PolitiqueTeletravailService {

    @Autowired private PolitiqueTeletravailRepository politiqueRepository;
    @Autowired private DemandeTeletravailRepository demandeRepository;
    @Autowired private UserRepository userRepository;

    // ── Chef définit ou met à jour le % de son équipe pour un mois donné ──
    public PolitiqueResponse definirPolitique(Equipe equipe, PolitiqueRequest request) {
        if (request.getPourcentageMaxSurSite() < 0 || request.getPourcentageMaxSurSite() > 100) {
            throw new IllegalArgumentException("Le pourcentage doit être entre 0 et 100");
        }

        PolitiqueTeletravail politique = politiqueRepository
                .findByEquipeIdAndAnneeAndMois(equipe.getId(), request.getAnnee(), request.getMois())
                .orElse(new PolitiqueTeletravail());

        politique.setEquipe(equipe);
        politique.setAnnee(request.getAnnee());
        politique.setMois(request.getMois());
        politique.setPourcentageMaxSurSite(request.getPourcentageMaxSurSite());
        System.out.println("POLITIQUE BEFORE SAVE = " + politique);
        PolitiqueTeletravail saved = politiqueRepository.save(politique);
        System.out.println("SAVED ID = " + saved.getId());
        return buildResponse(saved, equipe);
    }

    // ── Lecture du % pour un mois donné ──
    public PolitiqueResponse getPolitique(Equipe equipe, int annee, int mois) {

        PolitiqueTeletravail politique = politiqueRepository
                .findByEquipeIdAndAnneeAndMois(equipe.getId(), annee, mois)
                .orElse(null);

        int totalMembres = (equipe.getMembres() != null)
                ? equipe.getMembres().size()
                : 0;

        if (politique == null) {

            PolitiqueResponse response = new PolitiqueResponse();
            response.setNomEquipe(equipe.getNom());
            response.setAnnee(annee);
            response.setMois(mois);

            // valeur métier par défaut (IMPORTANT)
            response.setPourcentageMaxSurSite(60);

            response.setTotalMembres(totalMembres);
            response.setMembresEnTeletravail(0);
            response.setPourcentageActuel(0.0);

            return response;
        }

        return buildResponse(politique, equipe);
    }

    public Map<String, Object> verifierDisponibilite(User employe,
                                                     LocalDateTime dateDebut,
                                                     LocalDateTime dateFin) {
        Equipe equipe = employe.getEquipe();

        if (equipe == null) {
            return Map.of("autorise", true, "message", "Aucune équipe associée");
        }

        // Use FULL team count for policy calculation
        int totalMembres = userRepository.countByEquipeId(equipe.getId());

        if (totalMembres <= 1) {
            return Map.of("autorise", true, "message", "Équipe trop petite pour appliquer la politique");
        }

        int annee = dateDebut.getYear();
        int mois = dateDebut.getMonthValue();
        int anneeFin = dateFin.getYear();
        int moisFin = dateFin.getMonthValue();

        while (annee < anneeFin || (annee == anneeFin && mois <= moisFin)) {

            PolitiqueTeletravail politique = politiqueRepository
                    .findByEquipeIdAndAnneeAndMois(equipe.getId(), annee, mois)
                    .orElse(null);

            if (politique != null) {
                // Max teleworkers based on full team size and policy percentage
                int maxEnTeletravail = (int) Math.floor(
                        totalMembres * (100.0 - politique.getPourcentageMaxSurSite()) / 100.0
                );

                // One slot is reserved for the current requester
                int slotsDisponibles = maxEnTeletravail - 1;

                LocalDateTime debutMois = LocalDateTime.of(annee, mois, 1, 0, 0);
                LocalDateTime finMois = debutMois.plusMonths(1).minusSeconds(1);

                long dejaTeletravail = demandeRepository.findByUtilisateurEquipeId(equipe.getId())
                        .stream()
                        .filter(d -> StatutDemande.APPROVED.equals(d.getStatut()))
                        .filter(d -> !d.getUtilisateur().getId().equals(employe.getId()))
                        .filter(d -> d.getDateDebut() != null && d.getDateFin() != null)
                        .filter(d -> !d.getDateDebut().isAfter(finMois)
                                && !d.getDateFin().isBefore(debutMois))
                        .map(d -> d.getUtilisateur().getId())
                        .distinct()
                        .count();

                double score = totalMembres > 0
                        ? 100 - ((dejaTeletravail * 100.0) / totalMembres)
                        : 100;

                return Map.of(
                        "score", score,
                        "mois", mois,
                        "annee", annee,
                        "actuel", dejaTeletravail,
                        "total", totalMembres,
                        "message", "Score de disponibilité calculé"
                );
            }

            mois++;
            if (mois > 12) {
                mois = 1;
                annee++;
            }
        }

        return Map.of("autorise", true, "message", "Quota disponible");
    }
    // ==================== SCORE JOURNALIER ====================

    public Map<String, Object> calculerScoreJour(Equipe equipe, LocalDate date) {
        LocalDateTime debut = date.atStartOfDay();
        LocalDateTime fin = date.atTime(23, 59, 59);

        List<DemandeTeletravail> demandes =
                demandeRepository.findByUtilisateurEquipeId(equipe.getId());

        long enTeletravail = demandes.stream()
                .filter(d -> StatutDemande.APPROVED.equals(d.getStatut()))
                .filter(d -> !d.getDateDebut().isAfter(fin)
                        && !d.getDateFin().isBefore(debut))
                .map(d -> d.getUtilisateur().getId())
                .distinct()
                .count();

        int total = userRepository.countByEquipeId(equipe.getId());

        double scoreJour = total > 0
                ? 100 - (enTeletravail * 100.0 / total)
                : 100;

        return Map.of(
                "date", date,
                "scoreJour", scoreJour,
                "teletravail", enTeletravail,
                "total", total
        );
    }
    public Map<String, Object> calculerScoreMois(Equipe equipe, int annee, int mois) {

        LocalDateTime debut = LocalDateTime.of(annee, mois, 1, 0, 0);
        LocalDateTime fin = debut.plusMonths(1).minusSeconds(1);

        List<DemandeTeletravail> demandes =
                demandeRepository.findByUtilisateurEquipeId(equipe.getId());

        long enTeletravail = demandes.stream()
                .filter(d -> StatutDemande.APPROVED.equals(d.getStatut()))
                .filter(d -> !d.getDateDebut().isAfter(fin)
                        && !d.getDateFin().isBefore(debut))
                .map(d -> d.getUtilisateur().getId())
                .distinct()
                .count();

        int total = userRepository.countByEquipeId(equipe.getId());

        double score = total > 0
                ? 100 - (enTeletravail * 100.0 / total)
                : 100;

        return Map.of(
                "annee", annee,
                "mois", mois,
                "scoreMois", score,
                "teletravail", enTeletravail,
                "total", total
        );
    }
    // ── Construction du DTO de réponse enrichi ──
    private PolitiqueResponse buildResponse(PolitiqueTeletravail politique, Equipe equipe) {
        PolitiqueResponse response = new PolitiqueResponse();
        response.setId(politique.getId());
        response.setAnnee(politique.getAnnee());
        response.setMois(politique.getMois());
        response.setPourcentageMaxSurSite(politique.getPourcentageMaxSurSite());
        response.setNomEquipe(equipe.getNom());

        int totalMembres = userRepository.countByEquipeId(equipe.getId());
        response.setTotalMembres(totalMembres);

        // Calcul du nombre actuel en télétravail ce mois
        LocalDateTime debutMois = LocalDateTime.of(politique.getAnnee(), politique.getMois(), 1, 0, 0);
        LocalDateTime finMois   = debutMois.plusMonths(1).minusSeconds(1);

        long enTeletravail = demandeRepository.findByUtilisateurEquipeId(equipe.getId())
                .stream()
                .filter(d -> StatutDemande.APPROVED.equals(d.getStatut()))
                .filter(d -> d.getDateDebut() != null && d.getDateFin() != null)
                .filter(d -> !d.getDateDebut().isAfter(finMois)
                        && !d.getDateFin().isBefore(debutMois))
                .map(d -> d.getUtilisateur().getId())
                .distinct()
                .count();

        response.setMembresEnTeletravail((int) enTeletravail);
        response.setPourcentageActuel(totalMembres > 0 ? (enTeletravail * 100.0) / totalMembres : 0);

        return response;
    }

}