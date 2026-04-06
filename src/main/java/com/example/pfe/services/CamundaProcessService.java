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

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private DemandeTeletravailRepository demandeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // ==================== DEMANDES ====================

    // Créer une demande et démarrer le processus Camunda
    public Map<String, Object> createDemandeWithProcess(String motif, String dateDebut,
                                                        String dateFin, String type,
                                                        MultipartFile fichier, User employe) {

        // 1. Sauvegarder la demande en base
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

        // 2. Démarrer le processus Camunda
        Map<String, Object> variables = new HashMap<>();
        variables.put("demandeId", savedDemande.getId());
        variables.put("motif", motif);
        variables.put("dateDebut", dateDebut);
        variables.put("dateFin", dateFin);
        variables.put("employeEmail", employe.getEmail());
        variables.put("employeNom", employe.getNom());
        variables.put("chefApprouve", false);
        variables.put("adminApprouve", false);

        // Récupérer le chef d'équipe
        String chefEmail = getChefEmail(employe);
        variables.put("chefEmail", chefEmail);
        variables.put("adminEmail", "admin@siga.com");

        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("validation-teletravail", variables);

        // 3. Mettre à jour la demande avec l'ID du processus
        savedDemande.setProcessInstanceId(processInstance.getId());
        demandeRepository.save(savedDemande);

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

    // Récupérer les demandes par utilisateur
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

    // Récupérer toutes les demandes
    public List<DemandeTeletravailResponse> getAllDemandes() {
        return demandeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Suivi d'une demande
    public Map<String, Object> getSuiviDemande(Long demandeId, User currentUser) {
        DemandeTeletravail demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        // Vérifier les droits
        checkAccess(demande, currentUser);

        Map<String, Object> suivi = new HashMap<>();
        suivi.put("demande", mapToResponse(demande));

        // Récupérer l'état du processus Camunda
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

    // Annuler une demande
    public void annulerDemande(Long demandeId, User currentUser) {
        DemandeTeletravail demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (!demande.getUtilisateur().getId().equals(currentUser.getId()) && !currentUser.estAdmin()) {
            throw new RuntimeException("Vous ne pouvez annuler que vos propres demandes");
        }

        if (demande.getStatut() != StatutDemande.PENDING) {
            throw new RuntimeException("Seules les demandes en attente peuvent être annulées");
        }


        demandeRepository.save(demande);

        // Supprimer le processus Camunda si existe
        if (demande.getProcessInstanceId() != null) {
            runtimeService.deleteProcessInstance(demande.getProcessInstanceId(), "Annulée par l'utilisateur");
        }
    }

    // ==================== TÂCHES CAMUNDA ====================

    // Récupérer les tâches du chef
    // Ajoutez cette méthode si elle n'existe pas
    public List<TaskDto> getChefTasks(String chefEmail) {
        System.out.println("🔍 Recherche tâches pour chef: " + chefEmail);

        // Récupérer TOUTES les tâches ChefValidation
        List<Task> tasks = taskService.createTaskQuery()
                .taskDefinitionKey("ChefValidation")
                .list();

        System.out.println("📋 Tâches ChefValidation trouvées: " + tasks.size());

        return tasks.stream()
                .map(task -> {
                    TaskDto dto = new TaskDto(task);
                    try {
                        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
                        if (variables.get("demandeId") != null) {
                            dto.setDemandeId(((Number) variables.get("demandeId")).longValue());
                        }
                        dto.setMotif((String) variables.get("motif"));
                    } catch (Exception e) {
                        System.err.println("Erreur: " + e.getMessage());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
    public List<TaskDto> getAdminTasks() {
        System.out.println("=== getAdminTasks ===");

        // Récupérer TOUTES les tâches AdminValidation
        List<Task> tasks = taskService.createTaskQuery()
                .taskDefinitionKey("AdminValidation")
                .list();  // ← Pas de filtre

        System.out.println("📋 Tâches AdminValidation trouvées: " + tasks.size());

        return tasks.stream()
                .map(task -> {
                    TaskDto dto = new TaskDto(task);
                    try {
                        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
                        if (variables.get("demandeId") != null) {
                            dto.setDemandeId(((Number) variables.get("demandeId")).longValue());
                        }
                        dto.setMotif((String) variables.get("motif"));
                    } catch (Exception e) {
                        System.err.println("Erreur: " + e.getMessage());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Approuver une tâche
    public void approuverTache(String taskId, User validateur, String commentaire) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        Map<String, Object> variables = new HashMap<>();

        if (task.getTaskDefinitionKey().equals("ChefValidation")) {
            variables.put("chefApprouve", true);
            variables.put("chefCommentaire", commentaire);
        } else {
            variables.put("adminApprouve", true);
            variables.put("adminCommentaire", commentaire);

            // Si admin approuve, la demande est finalisée
            Long demandeId = (Long) runtimeService.getVariable(task.getProcessInstanceId(), "demandeId");
            Optional<DemandeTeletravail> demandeOpt = demandeRepository.findById(demandeId);
            if (demandeOpt.isPresent()) {
                DemandeTeletravail demande = demandeOpt.get();
                demande.setStatut(StatutDemande.APPROVED);
                demande.setValidateur(validateur);
                demandeRepository.save(demande);
            }
        }

        taskService.complete(taskId, variables);
    }

    // Rejeter une tâche
    public void rejeterTache(String taskId, User validateur, String commentaire) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        // La demande est rejetée définitivement
        Long demandeId = (Long) runtimeService.getVariable(task.getProcessInstanceId(), "demandeId");
        Optional<DemandeTeletravail> demandeOpt = demandeRepository.findById(demandeId);
        if (demandeOpt.isPresent()) {
            DemandeTeletravail demande = demandeOpt.get();
            demande.setStatut(StatutDemande.REJECTED);
            demande.setValidateur(validateur);
            demandeRepository.save(demande);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("rejected", true);
        variables.put("rejectionCommentaire", commentaire);
        taskService.complete(taskId, variables);
    }

    // ==================== STATISTIQUES ====================

    // Statistiques pour dashboard
    public Map<String, Object> getStatistiques(User currentUser) {
        List<DemandeTeletravailResponse> demandes = getDemandesByUser(currentUser);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", demandes.size());
        stats.put("pending", demandes.stream().filter(d -> d.getStatut() == StatutDemande.PENDING).count());
        stats.put("approved", demandes.stream().filter(d -> d.getStatut() == StatutDemande.APPROVED).count());
        stats.put("rejected", demandes.stream().filter(d -> d.getStatut() == StatutDemande.REJECTED).count());

        // Tâches en attente
        if (currentUser.estChef()) {
            stats.put("pendingTasks", getChefTasks(currentUser.getEmail()).size());
        }
        if (currentUser.estAdmin()) {
            stats.put("pendingAdminTasks", getAdminTasks().size());
        }

        return stats;
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private void checkAccess(DemandeTeletravail demande, User currentUser) {
        boolean hasAccess = currentUser.estAdmin() || currentUser.estRH() ||
                demande.getUtilisateur().getId().equals(currentUser.getId()) ||
                (currentUser.estChef() && currentUser.getEquipe() != null &&
                        demande.getUtilisateur().getEquipe() != null &&
                        currentUser.getEquipe().getId().equals(demande.getUtilisateur().getEquipe().getId()));

        if (!hasAccess) {
            throw new RuntimeException("Accès non autorisé à cette demande");
        }
    }

    private String getCurrentStep(String processInstanceId) {
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();

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

        if (demande.getUtilisateur() != null) {
            response.setUtilisateurNom(demande.getUtilisateur().getNom());
            response.setUtilisateurEmail(demande.getUtilisateur().getEmail());
        }

        return response;
    }
}