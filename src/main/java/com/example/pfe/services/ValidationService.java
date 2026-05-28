package com.example.pfe.services;

import com.example.pfe.models.*;
import com.example.pfe.repository.DemandeTeletravailRepository;
import com.example.pfe.repository.ValidationRepository;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.repository.EquipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    private final ValidationRepository validationRepository;
    private final DemandeTeletravailRepository demandeRepository;
    private final UserRepository userRepository;
    private final EquipeRepository equipeRepository;

    @Transactional
    public Validation ajouterValidation(Long demandeId, User validateur, StatutDemande statut, String commentaire) {
        DemandeTeletravail demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Vérifier si ce validateur a déjà voté
        boolean dejaVote = demande.getValidations().stream()
                .anyMatch(v -> v.getValidateur().getId().equals(validateur.getId()));

        if (dejaVote) {
            log.info("Validateur {} a déjà voté pour la demande {}", validateur.getEmail(), demandeId);
            // Retourner la validation existante au lieu de lancer une exception
            return demande.getValidations().stream()
                    .filter(v -> v.getValidateur().getId().equals(validateur.getId()))
                    .findFirst()
                    .orElseThrow();
        }

        // Vérifier que le validateur a le droit
        checkValidationPermission(demande, validateur);

        // Créer la validation
        Validation validation = new Validation();
        validation.setDemande(demande);
        validation.setValidateur(validateur);
        validation.setStatut(statut);
        validation.setCommentaire(commentaire);

        int etape = validateur.estChef() ? 1 : 2;
        validation.setEtape(etape);

        validationRepository.save(validation);
        demande.getValidations().add(validation);

        // Mettre à jour le statut de la demande
        updateDemandeStatus(demande);

        demandeRepository.save(demande);

        log.info("Validation ajoutée pour la demande {} par {}: {}",
                demandeId, validateur.getNom(), statut);

        return validation;
    }

    private void updateDemandeStatus(DemandeTeletravail demande) {
        List<Validation> validations = demande.getValidations();

        boolean chefVoted = validations.stream().anyMatch(v -> v.getValidateur().estChef());
        boolean adminVoted = validations.stream().anyMatch(v -> v.getValidateur().estAdmin());

        boolean chefApproved = validations.stream()
                .anyMatch(v -> v.getValidateur().estChef() && v.getStatut() == StatutDemande.APPROVED);
        boolean adminApproved = validations.stream()
                .anyMatch(v -> v.getValidateur().estAdmin() && v.getStatut() == StatutDemande.APPROVED);

        boolean anyRejected = validations.stream()
                .anyMatch(v -> v.getStatut() == StatutDemande.REJECTED);

        // Both have voted
        if (chefVoted && adminVoted) {
            // BPMN condition: approved if adminApprouve==true OR (adminApprouve==null && chefApprouve==true)
            boolean finalApproved = adminApproved || (!adminVoted && chefApproved);
            demande.setStatut(finalApproved ? StatutDemande.APPROVED : StatutDemande.REJECTED);
        } else {
            // Still waiting for remaining parallel vote
            demande.setStatut(StatutDemande.PENDING);
            log.info("Demande {} - en attente des votes parallèles ({}/2 reçus)",
                    demande.getId(), validations.size());
        }
    }

    private void checkValidationPermission(DemandeTeletravail demande, User validateur) {
        // ADMIN peut approuver/rejeter (même sans chef)
        if (validateur.estAdmin()) {
            return;
        }

        // CHEF_EQUIPE peut approuver/rejeter les demandes de son équipe
        if (validateur.estChef()) {
            Equipe equipe = validateur.getEquipe();
            User demandeur = demande.getUtilisateur();

            if (equipe == null) {
                throw new RuntimeException("Vous n'êtes pas assigné à une équipe");
            }
            if (demandeur.getEquipe() == null) {
                throw new RuntimeException("Le demandeur n'est pas assigné à une équipe");
            }
            if (!equipe.getId().equals(demandeur.getEquipe().getId())) {
                throw new RuntimeException("Vous ne pouvez valider que les demandes de votre équipe");
            }
            return;
        }

        throw new RuntimeException("Vous n'avez pas les droits pour valider cette demande");
    }

    public List<Validation> getHistoriqueValidations(Long demandeId) {
        return validationRepository.findByDemandeIdOrderByDateValidationAsc(demandeId);
    }

    public String getProchainValidateur(int etapeValidation) {
        switch (etapeValidation) {
            case 1:
                return "Chef d'équipe";
            case 2:
                return "Administrateur";
            default:
                return "Validation terminée";
        }
    }
}