package com.example.pfe.controllers;

import com.example.pfe.dto.EquipeRequest;
import com.example.pfe.dto.EquipeResponse;
import com.example.pfe.services.EquipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/equipes")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
@CrossOrigin(origins = "http://localhost:4200")
public class EquipeController {

    private final EquipeService equipeService;

    public EquipeController(EquipeService equipeService) {
        this.equipeService = equipeService;
    }

    @GetMapping
    public ResponseEntity<List<EquipeResponse>> getAllEquipes() {
        List<EquipeResponse> equipes = equipeService.getAllEquipes();
        System.out.println("=== Équipes retournées ===");
        equipes.forEach(e -> System.out.println("ID: " + e.getId() + ", Nom: " + e.getNom() + ", Chef: " + e.getChefNom()));
        return ResponseEntity.ok(equipes);
    }

    @PostMapping
    public ResponseEntity<?> createEquipe(@RequestBody EquipeRequest request) {
        try {
            EquipeResponse created = equipeService.createEquipe(request);
            System.out.println("=== Équipe créée: " + created.getNom() + " ===");
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            System.err.println("Erreur création équipe: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEquipe(@PathVariable Long id,
                                          @RequestBody EquipeRequest request) {
        try {
            EquipeResponse updated = equipeService.updateEquipe(id, request);
            System.out.println("=== Équipe mise à jour: " + updated.getNom() + " ===");
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            System.err.println("Erreur mise à jour équipe: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEquipe(@PathVariable Long id) {
        try {
            equipeService.deleteEquipe(id);
            System.out.println("=== Équipe supprimée ID: " + id + " ===");
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            System.err.println("Erreur suppression équipe: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}