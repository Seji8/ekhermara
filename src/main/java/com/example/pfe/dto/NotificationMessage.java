package com.example.pfe.dto;

import java.time.LocalDateTime;

public class NotificationMessage {
    private String type;        // "NEW_DEMANDE" | "STATUS_CHANGED"
    private Long demandeId;
    private String employeNom;
    private String statut;      // "APPROVED" | "REJECTED" | "PENDING"
    private String equipeId;    // to route to correct chef
    private LocalDateTime timestamp;
    private String message;
    private Long id;

    public NotificationMessage() {}

    public NotificationMessage(String type, Long demandeId, String employeNom,
                               String statut, String equipeId, String message) {
        this.type = type;
        this.demandeId = demandeId;
        this.employeNom = employeNom;
        this.statut = statut;
        this.equipeId = equipeId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // Getters & Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getDemandeId() { return demandeId; }
    public void setDemandeId(Long demandeId) { this.demandeId = demandeId; }
    public String getEmployeNom() { return employeNom; }
    public void setEmployeNom(String employeNom) { this.employeNom = employeNom; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getEquipeId() { return equipeId; }
    public void setEquipeId(String equipeId) { this.equipeId = equipeId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}