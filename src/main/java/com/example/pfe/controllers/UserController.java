package com.example.pfe.controllers;

import com.example.pfe.dto.CreateUserRequest;
import com.example.pfe.dto.CreateUserResponse;
import com.example.pfe.dto.UpdateUserRequest;
import com.example.pfe.models.Equipe;
import com.example.pfe.models.User;
import com.example.pfe.services.EquipeService;
import com.example.pfe.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final EquipeService equipeService;

    public UserController(UserService userService, EquipeService equipeService) {
        this.userService = userService;
        this.equipeService = equipeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            CreateUserResponse createdUser = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_EQUIPE')")
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Utilisateur non authentifié");
        }
        try {
            List<CreateUserResponse> users = userService.getUsersByRole(currentUser);
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/my-team")
    @PreAuthorize("hasRole('CHEF_EQUIPE')")
    public ResponseEntity<?> getMyTeam(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not authenticated");
        }
        try {
            Equipe equipe = userService.getEquipeForChef(currentUser);
            List<User> members = equipe.getMembres().stream()
                    .filter(m -> !m.getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(members);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_EQUIPE')")
    public ResponseEntity<?> getUserById(@PathVariable Long id,
                                         @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Utilisateur non authentifié");
        }
        try {
            CreateUserResponse user = userService.getUserById(id);
            if (currentUser.estChef()) {
                Equipe equipe = userService.getEquipeForChef(currentUser);
                if (user.getEquipeId() == null || !user.getEquipeId().equals(equipe.getId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Vous n'avez pas accès à cet utilisateur");
                }
            }
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        try {
            CreateUserResponse updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUserByEmail(@PathVariable String email) {
        try {
            userService.deleteUserByEmail(email);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @PutMapping("/me")
    
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE', 'EMPLOYE', 'RH')")
    public ResponseEntity<?> updateMyProfile(
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        try {
            // Force equipeId to stay unchanged — employee can't change their own team
            request.setRole(null);       // can't change own role
            request.setEquipeId(currentUser.getEquipe() != null
                    ? currentUser.getEquipe().getId() : null);

            CreateUserResponse updated = userService.updateUser(currentUser.getId(), request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}