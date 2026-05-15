package com.example.pfe.controllers;

import com.example.pfe.models.Notification;
import com.example.pfe.models.User;
import com.example.pfe.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyNotifications(
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            System.err.println("❌ currentUser null dans getMyNotifications");
            return ResponseEntity.status(401).build();
        }

        System.out.println("✅ getMyNotifications pour: " + currentUser.getEmail());

        List<Map<String, Object>> result = notificationRepository
                .findByDestinataireIdOrderByTimestampDesc(currentUser.getId())
                .stream()
                .map(n -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", n.getId().toString());
                    map.put("type", n.getType());
                    map.put("titre", n.getTitre());
                    map.put("message", n.getMessage());
                    map.put("demandeId", n.getDemandeId());
                    map.put("statut", n.getStatut());
                    map.put("timestamp", n.getTimestamp()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    map.put("lu", n.isLu());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/mark-read")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal User currentUser) {  // ✅ User object, pas String

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        List<Notification> notifs = notificationRepository
                .findByDestinataireIdOrderByTimestampDesc(currentUser.getId());
        notifs.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(notifs);
        return ResponseEntity.ok().build();
    }
}