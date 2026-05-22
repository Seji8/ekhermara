package com.example.pfe.services;

import com.example.pfe.dto.DemandeTeletravailRequest;
import com.example.pfe.dto.DemandeTeletravailResponse;
import com.example.pfe.models.DemandeTeletravail;
import com.example.pfe.models.StatutDemande;
import com.example.pfe.models.User;
import com.example.pfe.models.Validation;
import com.example.pfe.repository.DemandeTeletravailRepository;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.repository.ValidationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DemandeTeletravailService {

    private final DemandeTeletravailRepository demandeRepository;
    private final UserRepository userRepository;
    private final ValidationRepository validationRepository;
    private final ValidationService validationService;
    private final AuditService auditService;


    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public DemandeTeletravailService(DemandeTeletravailRepository demandeRepository,
                                     UserRepository userRepository,
                                     ValidationRepository validationRepository,
                                     ValidationService validationService, AuditService auditService) {
        this.demandeRepository = demandeRepository;
        this.userRepository = userRepository;
        this.validationRepository = validationRepository;
        this.validationService = validationService;
        this.auditService = auditService;
    }

    @Transactional
    public DemandeTeletravailResponse createDemande(DemandeTeletravailRequest request, Long userId) {
        User utilisateur = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        DemandeTeletravail demande = new DemandeTeletravail();
        demande.setMotif(request.getMotif());
        demande.setDateDebut(request.getDateDebut());
        demande.setDateFin(request.getDateFin());
        demande.setType(request.getType());
        demande.setStatut(StatutDemande.PENDING);
        demande.setUtilisateur(utilisateur);
        demande.setDateCreation(LocalDateTime.now());
        demande.setEtapeValidation(1);

        if (request.getFichier() != null && !request.getFichier().isEmpty()) {
            String fileName = saveFile(request.getFichier());
            demande.setFichierjustificatif(fileName);
        }

        DemandeTeletravail savedDemande = demandeRepository.save(demande);
        auditService.log("CREATE_DEMANDE", utilisateur, "Demande créée par: " + utilisateur.getNom());
        return mapToResponse(savedDemande);
    }

    @Transactional
    public DemandeTeletravailResponse updateDemande(Long id, DemandeTeletravailRequest request, Long userId) {
        DemandeTeletravail demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (!demande.getUtilisateur().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres demandes");
        }

        if (demande.getStatut() != StatutDemande.PENDING) {
            throw new RuntimeException("Vous ne pouvez modifier qu'une demande en attente");
        }

        demande.setMotif(request.getMotif());
        demande.setDateDebut(request.getDateDebut());
        demande.setDateFin(request.getDateFin());
        demande.setType(request.getType());

        if (request.getFichier() != null && !request.getFichier().isEmpty()) {
            if (demande.getFichierjustificatif() != null) {
                deleteFile(demande.getFichierjustificatif());
            }
            String fileName = saveFile(request.getFichier());
            demande.setFichierjustificatif(fileName);
        }

        DemandeTeletravail updatedDemande = demandeRepository.save(demande);
        return mapToResponse(updatedDemande);
    }

    @Transactional
    public DemandeTeletravailResponse approveDemande(Long id, Long validateurId) {
        DemandeTeletravail demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        User validateur = userRepository.findById(validateurId)
                .orElseThrow(() -> new RuntimeException("Validateur non trouvé"));

        validationService.ajouterValidation(id, validateur, StatutDemande.APPROVED, null);
        auditService.log("APPROVE_DEMANDE", validateur, "Demande #" + id + " approuvée par: " + validateur.getNom());

        DemandeTeletravail updatedDemande = demandeRepository.findById(id).orElseThrow();
        return mapToResponse(updatedDemande);
    }

    @Transactional
    public DemandeTeletravailResponse rejectDemande(Long id, Long validateurId) {
        DemandeTeletravail demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        User validateur = userRepository.findById(validateurId)
                .orElseThrow(() -> new RuntimeException("Validateur non trouvé"));

        validationService.ajouterValidation(id, validateur, StatutDemande.REJECTED, null);
        auditService.log("REJECT_DEMANDE", validateur, "Demande #" + id + " refusée par: " + validateur.getNom());

        DemandeTeletravail updatedDemande = demandeRepository.findById(id).orElseThrow();
        return mapToResponse(updatedDemande);
    }

    @Transactional
    public void deleteDemande(Long id, Long currentUserId) {
        DemandeTeletravail demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (!demande.getUtilisateur().getId().equals(currentUserId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette demande");
        }

        if (!demande.getStatut().equals(StatutDemande.PENDING)) {
            throw new RuntimeException("Seules les demandes en attente peuvent être annulées");
        }

        demandeRepository.delete(demande);
    }

    // ==================== MÉTHODES DE RECHERCHE ====================

    public List<DemandeTeletravailResponse> getAllDemandes() {
        return demandeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DemandeTeletravailResponse> getDemandesByUser(Long userId) {
        return demandeRepository.findByUtilisateurId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DemandeTeletravailResponse getDemandeById(Long id) {
        DemandeTeletravail demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        return mapToResponse(demande);
    }

    public DemandeTeletravail getDemandeEntityById(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
    }

    public List<DemandeTeletravailResponse> getDemandesByEquipe(Long equipeId) {
        List<User> membres = userRepository.findByEquipeId(equipeId);
        if (membres == null || membres.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = membres.stream()
                .map(User::getId)
                .collect(Collectors.toList());
        return demandeRepository.findByUtilisateurIdIn(userIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DemandeTeletravailResponse> getDemandesByEtapeValidation(int etape) {
        return demandeRepository.findByEtapeValidationAndStatut(etape, StatutDemande.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== MÉTHODES POUR LE SUIVI ====================

    public DemandeTeletravailResponse getDemandeDetail(Long id) {
        DemandeTeletravail demande = getDemandeEntityById(id);
        return mapToDetailResponse(demande);
    }

    private DemandeTeletravailResponse mapToDetailResponse(DemandeTeletravail demande) {
        DemandeTeletravailResponse response = new DemandeTeletravailResponse();

        response.setId(demande.getId());
        response.setMotif(demande.getMotif());
        response.setMotifRejet(demande.getMotifRejet());  // ← MANQUAIT
        response.setDateDebut(demande.getDateDebut());
        response.setDateFin(demande.getDateFin());
        response.setType(demande.getType());
        response.setStatut(demande.getStatut());
        response.setFichierjustificatif(demande.getFichierjustificatif());
        response.setDateCreation(demande.getDateCreation());
        response.setDuree(demande.getDuree());

        if (demande.getUtilisateur() != null) {
            User user = demande.getUtilisateur();
            response.setUtilisateurId(user.getId());
            response.setUtilisateurNom(user.getNom());
            response.setUtilisateurEmail(user.getEmail());
            response.setUtilisateurRole(user.getRole().name());
            if (user.getEquipe() != null) {
                response.setUtilisateurEquipe(user.getEquipe().getNom());
            }
        }

        if (demande.getValidateur() != null) {
            response.setValidateurNom(demande.getValidateur().getNom());
        }

        response.setEtapeValidation(demande.getEtapeValidation());

        if (demande.getValidations() != null && !demande.getValidations().isEmpty()) {
            List<DemandeTeletravailResponse.ValidationResponse> historique = demande.getValidations().stream()
                    .map(this::mapToValidationResponse)
                    .collect(Collectors.toList());
            response.setHistoriqueValidations(historique);
        }

        return response;
    }

    private DemandeTeletravailResponse.ValidationResponse mapToValidationResponse(Validation validation) {
        DemandeTeletravailResponse.ValidationResponse response = new DemandeTeletravailResponse.ValidationResponse();
        response.setId(validation.getId());
        response.setStatut(validation.getStatut().name());
        response.setCommentaire(validation.getCommentaire());
        response.setDateValidation(validation.getDateValidation());
        response.setEtapeValidation(validation.getEtape());

        if (validation.getValidateur() != null) {
            response.setValidateurNom(validation.getValidateur().getNom());
            response.setValidateurRole(validation.getValidateur().getRole().name());
        }

        return response;
    }

    private String getProchainValidateur(int etapeValidation) {
        switch (etapeValidation) {
            case 1:
                return "Chef d'équipe";
            case 2:
                return "Administrateur";
            default:
                return "Validation terminée";
        }
    }

    // ==================== MÉTHODES DE MAPPING ====================

    private DemandeTeletravailResponse mapToResponse(DemandeTeletravail demande) {
        DemandeTeletravailResponse response = new DemandeTeletravailResponse();
        response.setId(demande.getId());
        response.setMotif(demande.getMotif());
        response.setDateDebut(demande.getDateDebut());
        response.setDateFin(demande.getDateFin());
        response.setType(demande.getType());
        response.setStatut(demande.getStatut());
        response.setFichierjustificatif(demande.getFichierjustificatif());

        if (demande.getUtilisateur() != null) {
            User user = demande.getUtilisateur();
            response.setUtilisateurId(user.getId());
            response.setUtilisateurNom(user.getNom());
            response.setUtilisateurEmail(user.getEmail());
            if (user.getRole() != null) {
                response.setUtilisateurRole(user.getRole().name());
            }
            if (user.getEquipe() != null) {
                response.setUtilisateurEquipe(user.getEquipe().getNom());
            }
        }

        if (demande.getValidateur() != null) {
            response.setValidateurNom(demande.getValidateur().getNom());
        }

        response.setDateCreation(demande.getDateCreation());
        response.setDuree(demande.getDuree());
        response.setEtapeValidation(demande.getEtapeValidation());

        return response;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private String saveFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + fileExtension;

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier: " + e.getMessage());
        }
    }

    private void deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la suppression du fichier: " + e.getMessage());
        }
    }
}