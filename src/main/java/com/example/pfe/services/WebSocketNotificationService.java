package com.example.pfe.services;

import com.example.pfe.models.DemandeTeletravail;
import com.example.pfe.models.Notification;
import com.example.pfe.models.Role;
import com.example.pfe.models.User;
import com.example.pfe.repository.NotificationRepository;
import com.example.pfe.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate,
                                        NotificationRepository notificationRepository,
                                        UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ==================== NOUVELLE DEMANDE ====================
    // Notifie : Chef + ADMIN + RH

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifierNouvelleDemande(DemandeTeletravail demande, User employe) {
        System.out.println("🔔 notifierNouvelleDemande — employe: " + employe.getEmail());

        Map<String, Object> payload = buildPayload(
                "NEW_DEMANDE",
                "Nouvelle demande de télétravail",
                employe.getNom() + " a soumis une demande de télétravail",
                demande.getId(), null
        );

        // Notifier le chef d'équipe
        if (employe.getEquipe() != null) {
            User chef = employe.getEquipe().getChef();
            if (chef != null) {
                saveAndSend(payload, chef);
                messagingTemplate.convertAndSend(
                        "/topic/chef/" + employe.getEquipe().getId() + "/notifications", payload);
            }
        }

        // Notifier ADMIN et RH
        List<User> adminsAndRH = userRepository.findByRoleIn(List.of(Role.ADMIN, Role.RH));
        adminsAndRH.forEach(admin -> saveAndSend(payload, admin));
        messagingTemplate.convertAndSend("/topic/admin/notifications", payload);
    }

    // ==================== DÉCISION ====================
    // Notifie : Employé + Chef + ADMIN + RH

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifierDecision(DemandeTeletravail demande, String statut, String commentaire) {
        System.out.println("🔔 notifierDecision — statut: " + statut
                + " demande: " + demande.getId());

        boolean approved = "APPROVED".equals(statut);
        String titre = approved ? " Demande approuvée" : " Demande refusée";

        User employe = demande.getUtilisateur();

        // Payload spécifique pour l'employé (message personnel)
        Map<String, Object> payloadEmploye = buildPayload(
                "DECISION", titre,
                approved ? "Votre demande de télétravail a été approuvée"
                        : "Votre demande de télétravail a été refusée"
                          + (commentaire != null ? " : " + commentaire : ""),
                demande.getId(), statut
        );

        // Payload pour le staff (message informatif)
        Map<String, Object> payloadStaff = buildPayload(
                "DECISION", titre,
                "La demande de " + employe.getNom() + " a été "
                        + (approved ? "approuvée" : "refusée")
                        + (commentaire != null ? " : " + commentaire : ""),
                demande.getId(), statut
        );

        // 1. Notifier l'employé
        saveAndSend(payloadEmploye, employe);

        // 2. Notifier le chef d'équipe
        if (employe.getEquipe() != null) {
            User chef = employe.getEquipe().getChef();
            if (chef != null && !chef.getId().equals(employe.getId())) {
                saveAndSend(payloadStaff, chef);
                messagingTemplate.convertAndSend(
                        "/topic/chef/" + employe.getEquipe().getId() + "/notifications",
                        payloadStaff);
            }
        }

        // 3. Notifier ADMIN et RH
        List<User> adminsAndRH = userRepository.findByRoleIn(List.of(Role.ADMIN, Role.RH));
        adminsAndRH.forEach(admin -> {
            if (!admin.getId().equals(employe.getId())) {
                saveAndSend(payloadStaff, admin);
            }
        });
        messagingTemplate.convertAndSend("/topic/admin/notifications", payloadStaff);
    }

    // ==================== HELPERS ====================

    private void saveAndSend(Map<String, Object> payload, User destinataire) {
        try {
            System.out.println("💾 saveAndSend → " + destinataire.getEmail()
                    + " (id: " + destinataire.getId() + ")");

            Notification notif = new Notification();
            notif.setType((String) payload.get("type"));
            notif.setTitre((String) payload.get("titre"));
            notif.setMessage((String) payload.get("message"));
            notif.setStatut((String) payload.get("statut"));
            notif.setDestinataire(destinataire);
            notif.setLu(false);
            notif.setTimestamp(LocalDateTime.now());

            Object demandeId = payload.get("demandeId");
            if (demandeId != null) {
                notif.setDemandeId(((Number) demandeId).longValue());
            }

            Notification saved = notificationRepository.save(notif);
            System.out.println("Notif sauvegardée id: " + saved.getId());

            messagingTemplate.convertAndSendToUser(
                    destinataire.getEmail(),
                    "/queue/notifications",
                    payload
            );

        } catch (Exception e) {
            System.err.println(" Erreur saveAndSend: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, Object> buildPayload(String type, String titre,
                                             String message, Long demandeId, String statut) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type",      type);
        payload.put("titre",     titre);
        payload.put("message",   message);
        payload.put("demandeId", demandeId);
        payload.put("statut",    statut != null ? statut : "");
        payload.put("lu",        false);
        payload.put("timestamp", LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return payload;
    }
}