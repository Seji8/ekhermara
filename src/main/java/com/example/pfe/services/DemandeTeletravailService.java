package com.example.pfe.services;

import com.example.pfe.dto.DemandeTeletravailRequest;
import com.example.pfe.dto.DemandeTeletravailResponse;
import com.example.pfe.models.DemandeTeletravail;
import com.example.pfe.models.StatutDemande;
import com.example.pfe.models.User;
import com.example.pfe.repository.DemandeTeletravailRepository;
import com.example.pfe.repository.UserRepository;
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

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public DemandeTeletravailService(DemandeTeletravailRepository demandeRepository,
                                     UserRepository userRepository) {
        this.demandeRepository = demandeRepository;
        this.userRepository = userRepository;
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

        // Gérer l'upload du fichier
        if (request.getFichier() != null && !request.getFichier().isEmpty()) {
            String fileName = saveFile(request.getFichier());
            demande.setFichierjustificatif(fileName);
        }

        DemandeTeletravail savedDemande = demandeRepository.save(demande);
        return mapToResponse(savedDemande);
    }

    @Transactional
    public DemandeTeletravailResponse updateDemande(Long id, DemandeTeletravailRequest request, Long userId) {
        DemandeTeletravail demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Vérifier que l'utilisateur est le propriétaire
        if (!demande.getUtilisateur().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres demandes");
        }

        // Ne peut modifier que si c'est en attente
        if (demande.getStatut() != StatutDemande.PENDING) {
            throw new RuntimeException("Vous ne pouvez modifier qu'une demande en attente");
        }

        demande.setMotif(request.getMotif());
        demande.setDateDebut(request.getDateDebut());
        demande.setDateFin(request.getDateFin());
        demande.setType(request.getType());

        // Gérer l'upload du nouveau fichier
        if (request.getFichier() != null && !request.getFichier().isEmpty()) {
            // Supprimer l'ancien fichier si existe
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

        demande.setStatut(StatutDemande.APPROVED);
        demande.setValidateur(validateur);

        DemandeTeletravail approvedDemande = demandeRepository.save(demande);
        return mapToResponse(approvedDemande);
    }

    @Transactional
    public DemandeTeletravailResponse rejectDemande(Long id, Long validateurId) {
        DemandeTeletravail demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        User validateur = userRepository.findById(validateurId)
                .orElseThrow(() -> new RuntimeException("Validateur non trouvé"));

        demande.setStatut(StatutDemande.REJECTED);
        demande.setValidateur(validateur);

        DemandeTeletravail rejectedDemande = demandeRepository.save(demande);
        return mapToResponse(rejectedDemande);
    }

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

    @Transactional
    public void deleteDemande(Long id, Long userId) {
        DemandeTeletravail demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Vérifier que l'utilisateur est le propriétaire
        if (!demande.getUtilisateur().getId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez supprimer que vos propres demandes");
        }

        // Supprimer le fichier associé
        if (demande.getFichierjustificatif() != null) {
            deleteFile(demande.getFichierjustificatif());
        }

        demandeRepository.delete(demande);
    }

    private String saveFile(MultipartFile file) {
        try {
            // Créer le répertoire d'upload s'il n'existe pas
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Vérifier le type de fichier
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));

            // Générer un nom de fichier unique
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Sauvegarder le fichier
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

    private DemandeTeletravailResponse mapToResponse(DemandeTeletravail demande) {
        DemandeTeletravailResponse response = new DemandeTeletravailResponse();
        response.setId(demande.getId());
        response.setMotif(demande.getMotif());
        response.setDateDebut(demande.getDateDebut());
        response.setDateFin(demande.getDateFin());
        response.setType(demande.getType());
        response.setStatut(demande.getStatut());
        response.setFichierjustificatif(demande.getFichierjustificatif());
        response.setUtilisateurId(demande.getUtilisateur().getId());
        response.setUtilisateurNom(demande.getUtilisateur().getNom());

        if (demande.getValidateur() != null) {
            response.setValidateurNom(demande.getValidateur().getNom());
        }

        response.setDateCreation(demande.getDateCreation());
        response.setDuree(demande.getDuree());

        return response;
    }
}