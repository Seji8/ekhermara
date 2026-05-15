package com.example.pfe.services;

import com.example.pfe.dto.AuditLogResponse;
import com.example.pfe.models.AuditLog;
import com.example.pfe.models.User;
import com.example.pfe.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log an action performed on/by a user.
     *
     * @param action  e.g. "CREATE_USER", "UPDATE_USER", "DELETE_USER", "LOGIN", "DEMANDE_APPROVED"
     * @param user    the user the action concerns (can be null for system actions)
     * @param details free-text description
     */
    public void log(String action, User user, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setUser(user);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }

    public List<AuditLogResponse> getAll() {
        return auditLogRepository.findAllByOrderByDateDesc()
                .stream().map(AuditLogResponse::new).collect(Collectors.toList());
    }

    public List<AuditLogResponse> getFiltered(String action, Long userId,
                                               LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findFiltered(action, userId, from, to)
                .stream().map(AuditLogResponse::new).collect(Collectors.toList());
    }
}
