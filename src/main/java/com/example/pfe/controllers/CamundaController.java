package com.example.pfe.controllers;

import com.example.pfe.dto.DemandeTeletravailResponse;
import com.example.pfe.dto.TaskDto;
import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.services.CamundaProcessService;
import com.example.pfe.services.DemandeTeletravailService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/camunda")
@CrossOrigin(origins = "http://localhost:4200")
public class CamundaController {

    @Autowired private CamundaProcessService camundaService;
    @Autowired private UserRepository userRepository;
    @Autowired private DemandeTeletravailService demandeService;
    @Autowired private org.camunda.bpm.engine.RuntimeService runtimeService;
    @Autowired private org.camunda.bpm.engine.TaskService taskService;
    // ✅ Helper commun pour récupérer l'utilisateur courant
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        String email = principal.toString();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    // ==================== DEMANDES ====================

    @PostMapping("/demandes")
    public ResponseEntity<?> createDemande(
            @RequestParam("motif") String motif,
            @RequestParam("dateDebut") String dateDebut,
            @RequestParam("dateFin") String dateFin,
            @RequestParam("type") String type,
            @RequestParam(value = "fichier", required = false) MultipartFile fichier) {
        try {
            User user = getCurrentUser();
            Map<String, Object> result = camundaService.createDemandeWithProcess(
                    motif, dateDebut, dateFin, type, fichier, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/demandes")
    public ResponseEntity<List<DemandeTeletravailResponse>> getMyDemandes() {
        try {
            User user = getCurrentUser();
            return ResponseEntity.ok(camundaService.getDemandesByUser(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/demandes/all")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RH')")
    public ResponseEntity<List<DemandeTeletravailResponse>> getAllDemandes() {
        return ResponseEntity.ok(camundaService.getAllDemandes());
    }

    @GetMapping("/demandes/{id}/suivi")
    public ResponseEntity<?> getSuiviDemande(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            DemandeTeletravailResponse detail = demandeService.getDemandeDetail(id);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    @DeleteMapping("/demandes/{id}")
    public ResponseEntity<?> annulerDemande(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            camundaService.annulerDemande(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Demande annulée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== TÂCHES ====================

    @GetMapping("/taches/chef")
    public ResponseEntity<List<TaskDto>> getChefTasks() {
        try {
            User user = getCurrentUser();
            return ResponseEntity.ok(camundaService.getChefTasks(user.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/taches/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<TaskDto>> getAdminTasks() {
        return ResponseEntity.ok(camundaService.getAdminTasks());
    }

    @PostMapping("/taches/{taskId}/approuver")
    @PreAuthorize("hasAnyAuthority('ROLE_CHEF_EQUIPE', 'ROLE_ADMIN')")
    public ResponseEntity<?> approuverTache(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            User currentUser = getCurrentUser();
            String commentaire = body != null ? body.get("commentaire") : null;

            // ✅ action Camunda
            camundaService.approuverTache(taskId, currentUser, commentaire);

            // ✅ recalcul score propre
            Task task = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult();

            if (task != null) {
                Long demandeId = (Long) runtimeService.getVariable(
                        task.getProcessInstanceId(),
                        "demandeId"
                );

                camundaService.calculerScore(demandeId);
            }

            return ResponseEntity.ok(Map.of("message", "Tâche approuvée avec succès"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/taches/{taskId}/rejeter")
    @PreAuthorize("hasAnyAuthority('ROLE_CHEF_EQUIPE', 'ROLE_ADMIN')")
    public ResponseEntity<?> rejeterTache(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            User currentUser = getCurrentUser();
            String commentaire = body != null ? body.get("commentaire") : null;

            camundaService.rejeterTache(taskId, currentUser, commentaire);

            return ResponseEntity.ok(Map.of("message", "Tâche rejetée avec succès"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/demandes/{id}/score")
    public ResponseEntity<Map<String, Object>> getScore(@PathVariable Long id) {
        return ResponseEntity.ok(camundaService.calculerScore(id));
    }

    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> getStatistiques() {
        return ResponseEntity.ok(camundaService.getStatistiques(getCurrentUser()));
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/" + filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + filename + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/demandes/equipe")
    @PreAuthorize("hasAuthority('ROLE_CHEF_EQUIPE')")
    public ResponseEntity<?> getDemandesEquipe() {
        try {
            User chef = getCurrentUser();
            if (chef.getEquipe() == null) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(demandeService.getDemandesByEquipe(chef.getEquipe().getId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API Camunda fonctionne!");
    }
}