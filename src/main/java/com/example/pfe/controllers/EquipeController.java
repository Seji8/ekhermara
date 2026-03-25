package com.example.pfe.controllers;

import com.example.pfe.dto.EquipeResponse;
import com.example.pfe.services.EquipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/equipes")
@PreAuthorize("hasRole('ADMIN')")
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
}