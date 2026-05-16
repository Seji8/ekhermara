package com.example.pfe.dto;

import java.util.Date;
public class TaskDto {
    private String id;
    private String name;
    private String assignee;
    private Date createTime;
    private String processInstanceId;
    private String taskDefinitionKey;
    private String description;
    private int priority;
    private Long demandeId;  // Ajouté
    private String motif;     // Ajouté
    private String utilisateurNom;
    private String dateDebut;
    private String dateFin;
    private Long duree;

    public TaskDto() {}

    public TaskDto(org.camunda.bpm.engine.task.Task task) {
        this.id = task.getId();
        this.name = task.getName();
        this.assignee = task.getAssignee();
        this.createTime = task.getCreateTime();
        this.processInstanceId = task.getProcessInstanceId();
        this.taskDefinitionKey = task.getTaskDefinitionKey();
        this.description = task.getDescription();
        this.priority = task.getPriority();
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getTaskDefinitionKey() { return taskDefinitionKey; }
    public void setTaskDefinitionKey(String taskDefinitionKey) { this.taskDefinitionKey = taskDefinitionKey; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public Long getDemandeId() { return demandeId; }
    public void setDemandeId(Long demandeId) { this.demandeId = demandeId; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getUtilisateurNom() { return utilisateurNom; }
    public void setUtilisateurNom(String utilisateurNom) { this.utilisateurNom = utilisateurNom; }
    public String getDateDebut() { return dateDebut; }
    public void setDateDebut(String dateDebut) { this.dateDebut = dateDebut; }
    public String getDateFin() { return dateFin; }
    public void setDateFin(String dateFin) { this.dateFin = dateFin; }
    public Long getDuree() { return duree; }
    public void setDuree(Long duree) { this.duree = duree; }

}