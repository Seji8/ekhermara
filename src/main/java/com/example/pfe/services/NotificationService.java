package com.example.pfe.services;

import com.example.pfe.models.Notification;
import com.example.pfe.models.User;
import com.example.pfe.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public NotificationService(SimpMessagingTemplate messagingTemplate,
                               NotificationRepository notificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }

    public void envoyerNotification(User destinataire, String type,
                                    String titre, String message, Long demandeId) {
        // 1. Persister en base
        Notification notif = new Notification();
        notif.setDestinataire(destinataire);
        notif.setType(type);
        notif.setTitre(titre);
        notif.setMessage(message);
        notif.setDemandeId(demandeId);
        notif.setTimestamp(LocalDateTime.now());
        notif.setLu(false);
        notificationRepository.save(notif);

        // 2. Envoyer en temps réel
        Map<String, Object> payload = Map.of(
                "id",        notif.getId().toString(),
                "type",      type,
                "titre",     titre,
                "message",   message,
                "demandeId", demandeId != null ? demandeId : 0L,
                "timestamp", notif.getTimestamp().toString(),
                "lu",        false
        );

        messagingTemplate.convertAndSendToUser(
                destinataire.getEmail(),
                "/queue/notifications",
                payload
        );
    }
}