package com.example.pfe.controllers;

import com.example.pfe.dto.AuditLogResponse;
import com.example.pfe.services.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "http://localhost:4200")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * GET /api/audit/logs
     * Optional filters: action, userId, from (ISO datetime), to (ISO datetime)
     * Accessible only by RH and ADMIN roles.
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        if (action == null && userId == null && from == null && to == null) {
            return ResponseEntity.ok(auditService.getAll());
        }
        return ResponseEntity.ok(auditService.getFiltered(action, userId, from, to));
    }
}
