package com.example.pfe.controllers;

import com.example.pfe.dto.DemandeTeletravailRequest;
import com.example.pfe.dto.DemandeTeletravailResponse;
import com.example.pfe.models.*;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.services.DemandeTeletravailService;
import com.example.pfe.services.ValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "http://localhost:4200")
public class DemandeTeletravailController {

    private final DemandeTeletravailService demandeService;
    private final UserRepository userRepository;
    private final ValidationService validationService;  // ← Ajout

    public DemandeTeletravailController(DemandeTeletravailService demandeService,
                                        UserRepository userRepository,
                                        ValidationService validationService) {
        this.demandeService = demandeService;
        this.userRepository = userRepository;
        this.validationService = validationService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'RH', 'ADMIN')")
    public ResponseEntity<?> createDemande(
            @ModelAttribute DemandeTeletravailRequest request,
            @AuthenticationPrincipal User currentUser) {

        System.out.println("=== createDemande ===");
        System.out.println("currentUser: " + (currentUser != null ? currentUser.getEmail() : "null"));

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Utilisateur non authentifié");
        }

        try {
            DemandeTeletravailResponse created = demandeService.createDemande(request, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<DemandeTeletravailResponse>> getAllDemandes() {
        return ResponseEntity.ok(demandeService.getAllDemandes());
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'RH', 'ADMIN')")
    public ResponseEntity<List<DemandeTeletravailResponse>> getMyDemandes(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<DemandeTeletravailResponse> demandes;

        if (currentUser.estChef()) {
            Equipe equipe = currentUser.getEquipe();
            if (equipe != null) {
                demandes = demandeService.getDemandesByEquipe(equipe.getId());
            } else {
                demandes = List.of();
            }
        } else if (currentUser.estAdmin() || currentUser.estRH()) {
            demandes = demandeService.getAllDemandes();
        } else {
            demandes = demandeService.getDemandesByUser(currentUser.getId());
        }

        return ResponseEntity.ok(demandes);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE')")
    public ResponseEntity<?> updateDemande(
            @PathVariable Long id,
            @ModelAttribute DemandeTeletravailRequest request,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            DemandeTeletravailResponse updated = demandeService.updateDemande(id, request, currentUser.getId());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_EQUIPE')")
    public ResponseEntity<?> approveDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) Map<String, String> body) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String commentaire = body != null ? body.get("commentaire") : null;

            // Ajouter la validation
            Validation validation = validationService.ajouterValidation(
                    id,
                    currentUser,
                    StatutDemande.APPROVED,
                    commentaire
            );

            // Mettre à jour la demande
            DemandeTeletravailResponse demande = demandeService.approveDemande(id, currentUser.getId());

            return ResponseEntity.ok(demande);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_EQUIPE')")
    public ResponseEntity<?> rejectDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) Map<String, String> body) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String commentaire = body != null ? body.get("commentaire") : null;

            // Ajouter la validation
            Validation validation = validationService.ajouterValidation(
                    id,
                    currentUser,
                    StatutDemande.REJECTED,
                    commentaire
            );

            // Mettre à jour la demande
            DemandeTeletravailResponse demande = demandeService.rejectDemande(id, currentUser.getId());

            return ResponseEntity.ok(demande);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<?> deleteDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            demandeService.deleteDemande(id, currentUser.getId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/suivi")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'RH', 'ADMIN')")
    public ResponseEntity<?> getSuiviDemande(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Utilisateur non authentifié");
            }

            DemandeTeletravail demande = demandeService.getDemandeEntityById(id);

            boolean hasAccess = false;

            if (currentUser.estAdmin() || currentUser.estRH()) {
                hasAccess = true;
            } else if (demande.getUtilisateur().getId().equals(currentUser.getId())) {
                hasAccess = true;
            } else if (currentUser.estChef() && currentUser.getEquipe() != null &&
                    demande.getUtilisateur().getEquipe() != null &&
                    currentUser.getEquipe().getId().equals(demande.getUtilisateur().getEquipe().getId())) {
                hasAccess = true;
            }

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous n'avez pas accès à cette demande");
            }

            DemandeTeletravailResponse detail = demandeService.getDemandeDetail(id);
            return ResponseEntity.ok(detail);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération du suivi: " + e.getMessage());
        }
    }
}