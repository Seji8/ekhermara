package com.example.pfe.controllers;

import com.example.pfe.dto.PolitiqueRequest;
import com.example.pfe.dto.PolitiqueResponse;
import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;
import com.example.pfe.services.PolitiqueTeletravailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/politique")
@CrossOrigin(origins = "http://localhost:4200")
public class PolitiqueController {

    @Autowired private PolitiqueTeletravailService politiqueService;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof User) return (User) principal;
        return userRepository.findByEmail(principal.toString())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    @GetMapping("/score/jour")
    public ResponseEntity<?> scoreJour(@RequestParam String date) {
        User chef = getCurrentUser();

        if (chef.getEquipe() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Pas d'équipe"));
        }

        LocalDate d = LocalDate.parse(date);

        return ResponseEntity.ok(
                politiqueService.calculerScoreJour(chef.getEquipe(), d)
        );
    }
    @GetMapping("/score/mois")
    public ResponseEntity<?> scoreMois(@RequestParam int annee,
                                       @RequestParam int mois) {
        User chef = getCurrentUser();

        return ResponseEntity.ok(
                politiqueService.calculerScoreMois(chef.getEquipe(), annee, mois)
        );
    }

    // ── Chef définit le % de son équipe pour un mois ──
    @PostMapping("/definir")
    @PreAuthorize("hasAuthority('ROLE_CHEF_EQUIPE')")
    public ResponseEntity<?> definirPolitique(@RequestBody PolitiqueRequest request) {
        System.out.println("🔥 CONTROLLER HIT");
        System.out.println("REQUEST OK");
        System.out.println("ANNEE = " + request.getAnnee());
        System.out.println("MOIS = " + request.getMois());
        System.out.println("POURCENTAGE = " + request.getPourcentageMaxSurSite());
        try {
            User chef = getCurrentUser();
            if (chef.getEquipe() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Vous n'êtes associé à aucune équipe"));
            }
            PolitiqueResponse response = politiqueService.definirPolitique(chef.getEquipe(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Lecture du % pour un mois donné ──
    @GetMapping("/equipe")
    @PreAuthorize("hasAnyAuthority('ROLE_CHEF_EQUIPE', 'ROLE_ADMIN')")
    public ResponseEntity<?> getPolitique(
            @RequestParam int annee,
            @RequestParam int mois) {
        try {
            User chef = getCurrentUser();
            if (chef.getEquipe() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Aucune équipe associée"));
            }
            return ResponseEntity.ok(politiqueService.getPolitique(chef.getEquipe(), annee, mois));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Vérification AVANT soumission (appelé côté Angular avant d'afficher le form) ──
    @GetMapping("/verifier")
    public ResponseEntity<Map<String, Object>> verifierDisponibilite(
            @RequestParam String dateDebut,
            @RequestParam String dateFin) {
        try {
            User employe = getCurrentUser();
            Map<String, Object> result = politiqueService.verifierDisponibilite(
                    employe,
                    LocalDateTime.parse(dateDebut),
                    LocalDateTime.parse(dateFin));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}