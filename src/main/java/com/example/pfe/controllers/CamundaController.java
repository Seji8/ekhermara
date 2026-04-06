// CamundaController.java - Version corrigée
package com.example.pfe.controllers;

import com.example.pfe.dto.DemandeTeletravailRequest;
import com.example.pfe.dto.DemandeTeletravailResponse;
import com.example.pfe.dto.TaskDto;
import com.example.pfe.models.DemandeTeletravail;
import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.services.CamundaProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.pfe.services.DemandeTeletravailService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/camunda")
@CrossOrigin(origins = "http://localhost:4200")
public class CamundaController {

    @Autowired
    private CamundaProcessService camundaService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DemandeTeletravailService demandeService;

    // ==================== DEMANDES ====================

    @PostMapping("/demandes")
    public ResponseEntity<?> createDemande(
            @RequestParam("motif") String motif,
            @RequestParam("dateDebut") String dateDebut,
            @RequestParam("dateFin") String dateFin,
            @RequestParam("type") String type,
            @RequestParam(value = "fichier", required = false) MultipartFile fichier,
            @AuthenticationPrincipal User currentUser) {

        User user = currentUser;
        if (user == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        }

        System.out.println("=== createDemande CALLED ===");
        System.out.println("motif: " + motif);
        System.out.println("currentUser: " + user.getEmail());

        try {
            Map<String, Object> result = camundaService.createDemandeWithProcess(
                    motif, dateDebut, dateFin, type, fichier, user
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/demandes")
    public ResponseEntity<List<DemandeTeletravailResponse>> getMyDemandes(
            @AuthenticationPrincipal User currentUser) {
        System.out.println("=== getMyDemandes CALLED ===");
        System.out.println("Current user from @AuthenticationPrincipal: " + (currentUser != null ? currentUser.getEmail() : "null"));

        // Alternative: récupérer du contexte
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Auth from context: " + (auth != null ? auth.getName() : "null"));
        System.out.println("Auth authorities: " + (auth != null ? auth.getAuthorities() : "null"));

        if (currentUser == null && auth != null) {
            String email = auth.getName();
            currentUser = userRepository.findByEmail(email).orElse(null);
            System.out.println("User retrieved from context: " + (currentUser != null ? currentUser.getEmail() : "null"));
        }

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<DemandeTeletravailResponse> demandes = camundaService.getDemandesByUser(currentUser);
        System.out.println("Demandes trouvées: " + demandes.size());

        return ResponseEntity.ok(demandes);
    }

    @GetMapping("/demandes/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<DemandeTeletravailResponse>> getAllDemandes() {
        return ResponseEntity.ok(camundaService.getAllDemandes());
    }

    @GetMapping("/demandes/{id}/suivi")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'RH', 'ADMIN')")
    public ResponseEntity<?> getSuiviDemande(@PathVariable Long id) {
        try {
            System.out.println("=== getSuiviDemande ===");
            System.out.println("Demande ID: " + id);

            // ✅ Récupérer l'utilisateur du contexte de sécurité
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                System.out.println("❌ Non authentifié");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
            }

            String email = auth.getName();
            System.out.println("Email from context: " + email);

            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("User found: " + currentUser.getEmail() + ", role: " + currentUser.getRole());

            // Récupérer la demande
            DemandeTeletravail demande = demandeService.getDemandeEntityById(id);

            // Vérifier les droits : ADMIN a toujours accès
            if (currentUser.estAdmin()) {
                System.out.println("✅ Admin - Accès autorisé");
                DemandeTeletravailResponse detail = demandeService.getDemandeDetail(id);
                return ResponseEntity.ok(detail);
            }

            // Vérifier les autres droits
            boolean hasAccess = currentUser.estRH() ||
                    demande.getUtilisateur().getId().equals(currentUser.getId()) ||
                    (currentUser.estChef() && currentUser.getEquipe() != null &&
                            demande.getUtilisateur().getEquipe() != null &&
                            currentUser.getEquipe().getId().equals(demande.getUtilisateur().getEquipe().getId()));

            if (!hasAccess) {
                System.out.println("❌ Accès refusé");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès non autorisé");
            }

            DemandeTeletravailResponse detail = demandeService.getDemandeDetail(id);
            return ResponseEntity.ok(detail);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur: " + e.getMessage());
        }
    }
    // Ajoutez cette méthode
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        System.out.println("=== TEST ENDPOINT CALLED ===");
        return ResponseEntity.ok("API Camunda fonctionne!");
    }
    @DeleteMapping("/demandes/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'ADMIN')")
    public ResponseEntity<?> annulerDemande(@PathVariable Long id,
                                            @AuthenticationPrincipal User currentUser) {
        try {
            camundaService.annulerDemande(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Demande annulée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== TÂCHES CAMUNDA ====================

    // ✅ CORRECTION : Méthode getChefTasks avec l'annotation @GetMapping
    @GetMapping("/taches/chef")
    public ResponseEntity<List<TaskDto>> getChefTasks() {
        System.out.println("=== getChefTasks ===");

        // Récupérer l'utilisateur du contexte
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = auth.getName();
        System.out.println("Email: " + email);

        // Déléguer au service
        List<TaskDto> tasks = camundaService.getChefTasks(email);
        System.out.println("Tâches trouvées: " + tasks.size());

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/taches/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TaskDto>> getAdminTasks() {
        List<TaskDto> tasks = camundaService.getAdminTasks();
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/taches/{taskId}/approuver")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<?> approuverTache(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {

        try {
            String commentaire = body != null ? body.get("commentaire") : null;
            camundaService.approuverTache(taskId, currentUser, commentaire);
            return ResponseEntity.ok(Map.of("message", "Tâche approuvée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/taches/{taskId}/rejeter")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<?> rejeterTache(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {

        try {
            String commentaire = body != null ? body.get("commentaire") : null;
            camundaService.rejeterTache(taskId, currentUser, commentaire);
            return ResponseEntity.ok(Map.of("message", "Tâche rejetée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'CHEF_EQUIPE', 'RH', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getStatistiques(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(camundaService.getStatistiques(currentUser));
    }
}