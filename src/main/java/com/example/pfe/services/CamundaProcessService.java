package com.example.pfe.services;

import com.example.pfe.dto.DemandeTeletravailResponse;
import com.example.pfe.dto.TaskDto;
import com.example.pfe.models.*;
import com.example.pfe.repository.DemandeTeletravailRepository;
import com.example.pfe.repository.UserRepository;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CamundaProcessService {

    @Autowired private RuntimeService runtimeService;
    @Autowired private TaskService taskService;
    @Autowired private HistoryService historyService;
    @Autowired private DemandeTeletravailRepository demandeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private WebSocketNotificationService notificationService;

    // ==================== DEMANDES ====================

    public Map<String, Object> createDemandeWithProcess(String motif, String dateDebut,
                                                        String dateFin, String type,
                                                        MultipartFile fichier, User employe) {
        DemandeTeletravail demande = new DemandeTeletravail();
        demande.setMotif(motif);
        demande.setDateDebut(LocalDateTime.parse(dateDebut));
        demande.setDateFin(LocalDateTime.parse(dateFin));
        demande.setType(TypeDemande.valueOf(type.toUpperCase()));
        demande.setStatut(StatutDemande.PENDING);
        demande.setUtilisateur(employe);
        demande.setDateCreation(LocalDateTime.now());

        if (fichier != null && !fichier.isEmpty()) {
            String fileName = fileStorageService.saveFile(fichier);
            demande.setFichierjustificatif(fileName);
        }

        DemandeTeletravail savedDemande = demandeRepository.save(demande);

        Map<String, Object> variables = new HashMap<>();
        variables.put("demandeId", savedDemande.getId());
        variables.put("motif", motif);
        variables.put("dateDebut", dateDebut);
        variables.put("dateFin", dateFin);
        variables.put("employeEmail", employe.getEmail());
        variables.put("employeNom", employe.getNom());
        variables.put("chefApprouve", false);
        variables.put("adminApprouve", false);

        String chefEmail = getChefEmail(employe);
        variables.put("chefEmail", chefEmail);
        variables.put("adminEmail", "admin@siga.com");

        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("validation-teletravail-v2", variables);

        savedDemande.setProcessInstanceId(processInstance.getId());
        demandeRepository.save(savedDemande);

        // ✅ Notifier chef + admin + RH
        notificationService.notifierNouvelleDemande(savedDemande, employe);

        Map<String, Object> result = new HashMap<>();
        result.put("demande", mapToResponse(savedDemande));
        result.put("processInstanceId", processInstance.getId());
        result.put("message", "Demande créée et workflow Camunda démarré");
        return result;
    }

    private String getChefEmail(User employe) {
        if (employe.getEquipe() != null && employe.getEquipe().getChef() != null) {
            return employe.getEquipe().getChef().getEmail();
        }
        return "admin@siga.com";
    }

    public List<DemandeTeletravailResponse> getDemandesByUser(User user) {
        List<DemandeTeletravail> demandes;

        if (user.estAdmin() || user.estRH()) {
            demandes = demandeRepository.findAll();
        } else if (user.estChef() && user.getEquipe() != null) {
            demandes = demandeRepository.findByUtilisateurEquipeId(user.getEquipe().getId());
        } else {
            demandes = demandeRepository.findByUtilisateurId(user.getId());
        }

        return demandes.stream()
                .sorted((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DemandeTeletravailResponse> getAllDemandes() {
        return demandeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getSuiviDemande(Long demandeId, User currentUser) {
        DemandeTeletravail demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        checkAccess(demande, currentUser);

        Map<String, Object> suivi = new HashMap<>();
        suivi.put("demande", mapToResponse(demande));

        if (demande.getProcessInstanceId() != null) {
            List<HistoricActivityInstance> historique = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(demande.getProcessInstanceId())
                    .orderByHistoricActivityInstanceEndTime().asc()
                    .list();

            suivi.put("historiqueCamunda", historique);
            suivi.put("etapeActuelle", getCurrentStep(demande.getProcessInstanceId()));
        }

        return suivi;
    }

    public void annulerDemande(Long demandeId, User currentUser) {
        DemandeTeletravail demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (!demande.getUtilisateur().getId().equals(currentUser.getId())
                && !currentUser.estAdmin()) {
            throw new RuntimeException("Vous ne pouvez annuler que vos propres demandes");
        }

        if (demande.getStatut() != StatutDemande.PENDING) {
            throw new RuntimeException("Seules les demandes en attente peuvent être annulées");
        }

        if (demande.getProcessInstanceId() != null) {
            try {
                runtimeService.deleteProcessInstance(
                        demande.getProcessInstanceId(), "Annulée par l'utilisateur");
            } catch (Exception e) {
                System.err.println("⚠️ Processus Camunda introuvable: " + e.getMessage());
            }
        }

        demandeRepository.delete(demande);
    }

    // ==================== TÂCHES CAMUNDA ====================

    public List<TaskDto> getChefTasks(String chefEmail) {
        System.out.println("🔍 Recherche tâches pour chef: " + chefEmail);

        List<Task> tasks = taskService.createTaskQuery()
                .taskDefinitionKey("ChefValidation")
                .list();

        System.out.println("📋 Tâches ChefValidation trouvées: " + tasks.size());

        return tasks.stream().map(task -> {
            TaskDto dto = new TaskDto(task);
            try {
                Map<String, Object> variables = runtimeService
                        .getVariables(task.getProcessInstanceId());
                if (variables.get("demandeId") != null) {
                    Long demandeId = ((Number) variables.get("demandeId")).longValue();
                    dto.setDemandeId(demandeId);
                    demandeRepository.findById(demandeId).ifPresent(demande -> {
                        dto.setMotif(demande.getMotif());
                        dto.setDateDebut(demande.getDateDebut() != null
                                ? demande.getDateDebut().toString() : null);
                        dto.setDateFin(demande.getDateFin() != null
                                ? demande.getDateFin().toString() : null);
                        dto.setDuree((long) demande.getDuree());
                        if (demande.getUtilisateur() != null) {
                            dto.setUtilisateurNom(demande.getUtilisateur().getNom());
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Erreur: " + e.getMessage());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public List<TaskDto> getAdminTasks() {
        System.out.println("=== getAdminTasks ===");

        List<Task> tasks = taskService.createTaskQuery()
                .taskDefinitionKey("AdminValidation")
                .list();

        System.out.println("📋 Tâches AdminValidation trouvées: " + tasks.size());

        return tasks.stream().map(task -> {
            TaskDto dto = new TaskDto(task);
            try {
                Map<String, Object> variables = runtimeService
                        .getVariables(task.getProcessInstanceId());
                if (variables.get("demandeId") != null) {
                    Long demandeId = ((Number) variables.get("demandeId")).longValue();
                    dto.setDemandeId(demandeId);
                    demandeRepository.findById(demandeId).ifPresent(demande -> {
                        dto.setMotif(demande.getMotif());
                        dto.setDateDebut(demande.getDateDebut() != null
                                ? demande.getDateDebut().toString() : null);
                        dto.setDateFin(demande.getDateFin() != null
                                ? demande.getDateFin().toString() : null);
                        dto.setDuree((long) demande.getDuree());
                        if (demande.getUtilisateur() != null) {
                            dto.setUtilisateurNom(demande.getUtilisateur().getNom());
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Erreur: " + e.getMessage());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public void approuverTache(String taskId, User validateur, String commentaire) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new RuntimeException("Tâche introuvable: " + taskId);

        String processInstanceId = task.getProcessInstanceId();
        Long demandeId = (Long) runtimeService.getVariable(processInstanceId, "demandeId");

        // Déjà traitée
        Optional<DemandeTeletravail> demandeOpt = demandeRepository.findById(demandeId);
        if (demandeOpt.isPresent() && demandeOpt.get().getStatut() != StatutDemande.PENDING) {
            taskService.complete(taskId, Map.of("chefApprouve", true, "adminApprouve", true));
            return;
        }

        // Compléter la tâche courante
        Map<String, Object> variables = new HashMap<>();
        variables.put("chefApprouve", true);
        variables.put("adminApprouve", true);
        if (task.getTaskDefinitionKey().equals("ChefValidation")) {
            variables.put("chefCommentaire", commentaire != null ? commentaire : "");
        } else {
            variables.put("adminCommentaire", commentaire != null ? commentaire : "");
        }
        taskService.complete(taskId, variables);

        // Compléter les tâches restantes
        taskService.createTaskQuery()
                .processInstanceId(processInstanceId).active().list()
                .forEach(t -> taskService.complete(t.getId(),
                        Map.of("chefApprouve", true, "adminApprouve", true)));

        // ✅ Sauvegarder statut + notifier tout le monde
        demandeRepository.findById(demandeId).ifPresent(demande -> {
            demande.setStatut(StatutDemande.APPROVED);
            demande.setValidateur(validateur);
            demandeRepository.save(demande);
            notificationService.notifierDecision(demande, "APPROVED", commentaire);
        });
    }

    public void rejeterTache(String taskId, User validateur, String commentaire) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new RuntimeException("Tâche introuvable: " + taskId);

        String processInstanceId = task.getProcessInstanceId();
        Long demandeId = (Long) runtimeService.getVariable(processInstanceId, "demandeId");

        // Déjà traitée
        Optional<DemandeTeletravail> demandeOpt = demandeRepository.findById(demandeId);
        if (demandeOpt.isPresent() && demandeOpt.get().getStatut() != StatutDemande.PENDING) {
            taskService.complete(taskId, Map.of("chefApprouve", false, "adminApprouve", false));
            return;
        }

        // Compléter la tâche courante
        Map<String, Object> variables = new HashMap<>();
        variables.put("chefApprouve", false);
        variables.put("adminApprouve", false);
        if (task.getTaskDefinitionKey().equals("ChefValidation")) {
            variables.put("chefCommentaire", commentaire != null ? commentaire : "");
        } else {
            variables.put("adminCommentaire", commentaire != null ? commentaire : "");
        }
        taskService.complete(taskId, variables);// Compléter les tâches restantes
        taskService.createTaskQuery()
                .processInstanceId(processInstanceId).active().list()
                .forEach(t -> taskService.complete(t.getId(),
                        Map.of("chefApprouve", false, "adminApprouve", false)));

        // ✅ Sauvegarder statut + notifier tout le monde
        demandeRepository.findById(demandeId).ifPresent(demande -> {
            demande.setStatut(StatutDemande.REJECTED);
            demande.setValidateur(validateur);
            demande.setMotifRejet(commentaire != null ? commentaire : "");
            demandeRepository.save(demande);

            notificationService.notifierDecision(demande, "REJECTED", commentaire);
        });
    }

    // ==================== STATISTIQUES ====================

    public Map<String, Object> getStatistiques(User currentUser) {
        List<DemandeTeletravailResponse> demandes = getDemandesByUser(currentUser);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", demandes.size());
        stats.put("pending", demandes.stream()
                .filter(d -> d.getStatut() == StatutDemande.PENDING).count());
        stats.put("approved", demandes.stream()
                .filter(d -> d.getStatut() == StatutDemande.APPROVED).count());
        stats.put("rejected", demandes.stream()
                .filter(d -> d.getStatut() == StatutDemande.REJECTED).count());

        if (currentUser.estChef()) {
            stats.put("pendingTasks", getChefTasks(currentUser.getEmail()).size());
        }
        if (currentUser.estAdmin()) {
            stats.put("pendingAdminTasks", getAdminTasks().size());
        }

        return stats;
    }

    // ==================== SCORING ====================

    public Map<String, Object> calculerScore(Long demandeId) {
        DemandeTeletravail demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        User utilisateur = demande.getUtilisateur();
        Equipe equipe = utilisateur.getEquipe();

        if (equipe == null) return buildScore(100, 0, 0);

        List<User> membres = equipe.getMembres().stream()
                .filter(m -> !m.getId().equals(utilisateur.getId()))
                .collect(Collectors.toList());

        int tailleEquipe = membres.size();
        if (tailleEquipe == 0) return buildScore(0, 0, 0);

        if (demande.getDateDebut() == null || demande.getDateFin() == null) {
            return buildScore(0, 0, tailleEquipe);
        }

        List<Long> membreIds = membres.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        long chevauchements = demandeRepository.findByUtilisateurIdIn(membreIds).stream()
                .filter(d -> StatutDemande.APPROVED.equals(d.getStatut()))
                .filter(d -> d.getDateDebut() != null && d.getDateFin() != null)
                .filter(d -> !d.getDateDebut().isAfter(demande.getDateFin())
                        && !d.getDateFin().isBefore(demande.getDateDebut()))
                .map(d -> d.getUtilisateur().getId())
                .distinct()
                .count();
        int score;

        if (tailleEquipe == 0) {
            score = 100;
        } else {
            double ratio = (chevauchements * 100.0) / tailleEquipe;

            score = (int) Math.round(100 - ratio);
        }
        return buildScore(score, chevauchements, tailleEquipe);
    }

    private Map<String, Object> buildScore(int score, long chevauchements, int tailleEquipe) {

        String niveau;
        String label;

        if (score >= 80) {
            niveau = "low";
            label = "Disponibilité élevée";
        } else if (score >= 50) {
            niveau = "medium";
            label = "Disponibilité moyenne";
        } else {
            niveau = "high";
            label = "Disponibilité faible";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("label", label);
        result.put("niveau", niveau);
        result.put("demandesEnChevauchement", chevauchements);
        result.put("tailleEquipe", tailleEquipe);

        return result;
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private void checkAccess(DemandeTeletravail demande, User currentUser) {
        boolean hasAccess = currentUser.estAdmin()
                || currentUser.estRH()
                || demande.getUtilisateur().getId().equals(currentUser.getId())
                || (currentUser.estChef() && currentUser.getEquipe() != null
                && demande.getUtilisateur().getEquipe() != null
                && currentUser.getEquipe().getId()
                .equals(demande.getUtilisateur().getEquipe().getId()));

        if (!hasAccess) throw new RuntimeException("Accès non autorisé à cette demande");
    }

    private String getCurrentStep(String processInstanceId) {
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId).active().list();
        if (activeTasks.isEmpty()) return "Terminé";
        return activeTasks.get(0).getName();
    }

    private DemandeTeletravailResponse mapToResponse(DemandeTeletravail demande) {
        DemandeTeletravailResponse response = new DemandeTeletravailResponse();
        response.setId(demande.getId());
        response.setMotif(demande.getMotif());
        response.setDateDebut(demande.getDateDebut());
        response.setDateFin(demande.getDateFin());
        response.setType(demande.getType());
        response.setStatut(demande.getStatut());
        response.setDateCreation(demande.getDateCreation());
        response.setProcessInstanceId(demande.getProcessInstanceId());
        response.setFichierjustificatif(demande.getFichierjustificatif());
        response.setMotifRejet(demande.getMotifRejet());
        if (demande.getUtilisateur() != null) {
            response.setUtilisateurNom(demande.getUtilisateur().getNom());
            response.setUtilisateurEmail(demande.getUtilisateur().getEmail());
            response.setUtilisateurId(demande.getUtilisateur().getId());
            if (demande.getUtilisateur().getEquipe() != null) {
                response.setUtilisateurEquipe(demande.getUtilisateur().getEquipe().getNom());
            }
        }

        return response;
    }
}