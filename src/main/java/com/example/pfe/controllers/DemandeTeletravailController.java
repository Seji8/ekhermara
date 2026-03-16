package com.example.pfe.controllers;

import com.example.pfe.dto.DemandeTeletravailRequest;
import com.example.pfe.dto.DemandeTeletravailResponse;
import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.services.DemandeTeletravailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "http://localhost:4200")
public class DemandeTeletravailController {

    private final DemandeTeletravailService demandeService;
    private final UserRepository userRepository;

    public DemandeTeletravailController(DemandeTeletravailService demandeService,UserRepository userRepository) {
        this.demandeService = demandeService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'RH', 'ADMIN')")
    public ResponseEntity<?> createDemande(
            @ModelAttribute DemandeTeletravailRequest request,
            @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest httpRequest) {
        try {
            // DEBUG: Voir ce qui est dans le contexte de sécurité
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("=== DEBUG AUTH ===");
            System.out.println("Auth object: " + auth);
            System.out.println("Principal: " + (auth != null ? auth.getPrincipal() : "null"));
            System.out.println("UserDetails param: " + userDetails);

            // Récupérer l'email depuis le SecurityContext directement
            String email;
            if (auth != null && auth.getPrincipal() instanceof String) {
                email = (String) auth.getPrincipal();
            } else if (userDetails != null) {
                email = userDetails.getUsername();
            } else {
                email = null;
            }

            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Utilisateur non authentifié");
            }

            // Trouver l'utilisateur dans la base
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec email: " + email));

            DemandeTeletravailResponse created = demandeService.createDemande(request, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (Exception e) {
            e.printStackTrace();  // ← Pour voir l'erreur complète
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<List<DemandeTeletravailResponse>> getAllDemandes() {
        return ResponseEntity.ok(demandeService.getAllDemandes());
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'RH', 'ADMIN')")
    public ResponseEntity<List<DemandeTeletravailResponse>> getMyDemandes(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(demandeService.getDemandesByUser(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'RH', 'ADMIN')")
    public ResponseEntity<?> getDemandeById(@PathVariable Long id) {
        try {
            DemandeTeletravailResponse demande = demandeService.getDemandeById(id);
            return ResponseEntity.ok(demande);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE')")
    public ResponseEntity<?> updateDemande(
            @PathVariable Long id,
            @ModelAttribute DemandeTeletravailRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getCurrentUserId(userDetails);
            DemandeTeletravailResponse updated = demandeService.updateDemande(id, request, userId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<?> approveDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long validateurId = getCurrentUserId(userDetails);
            DemandeTeletravailResponse approved = demandeService.approveDemande(id, validateurId);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<?> rejectDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long validateurId = getCurrentUserId(userDetails);
            DemandeTeletravailResponse rejected = demandeService.rejectDemande(id, validateurId);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<?> deleteDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getCurrentUserId(userDetails);
            demandeService.deleteDemande(id, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec email: " + email))
                .getId();
    }
}