package com.example.pfe.dto;

import com.example.pfe.models.AuditLog;

public class AuditLogResponse {
    private Long id;
    private String date;
    private String action;
    private Long userId;
    private String userName;
    private String userEmail;
    private String details;

    public AuditLogResponse(AuditLog log) {
        this.id      = log.getId();
        this.date    = log.getDate() != null ? log.getDate().toString() : null;
        this.action  = log.getAction();
        this.details = log.getDetails();

        if (log.getUser() != null) {
            this.userId    = log.getUser().getId();
            this.userName  = log.getUser().getNom();
            this.userEmail = log.getUser().getEmail();
        }
    }

    // Getters
    public Long getId()          { return id; }
    public String getDate()      { return date; }
    public String getAction()    { return action; }
    public Long getUserId()      { return userId; }
    public String getUserName()  { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getDetails()   { return details; }
}