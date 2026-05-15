package com.example.pfe.services;

import com.example.pfe.dto.EquipeRequest;
import com.example.pfe.dto.EquipeResponse;
import com.example.pfe.models.Equipe;
import com.example.pfe.models.User;
import com.example.pfe.repository.EquipeRepository;
import com.example.pfe.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final UserRepository userRepository;

    public EquipeService(EquipeRepository equipeRepository, UserRepository userRepository) {
        this.equipeRepository = equipeRepository;
        this.userRepository = userRepository;
        System.out.println("EquipeService initialisé");
    }

    public List<EquipeResponse> getAllEquipes() {
        System.out.println("getAllEquipes appelé");
        return equipeRepository.findAll().stream()
                .map(equipe -> {
                    System.out.println("Équipe: " + equipe.getNom() + ", Chef ID: " +
                            (equipe.getChef() != null ? equipe.getChef().getId() : "null"));
                    return new EquipeResponse(equipe);
                })
                .collect(Collectors.toList());
    }

    public EquipeResponse getEquipeById(Long id) {
        Equipe equipe = equipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Équipe non trouvée"));
        return new EquipeResponse(equipe);
    }

    @Transactional
    public EquipeResponse createEquipe(EquipeRequest request) {
        System.out.println("createEquipe appelé: " + request.getNom());

        // Vérifier si une équipe avec ce nom existe déjà
        if (equipeRepository.findByNom(request.getNom()).isPresent()) {
            throw new RuntimeException("Une équipe avec le nom '" + request.getNom() + "' existe déjà");
        }

        Equipe equipe = new Equipe();
        equipe.setNom(request.getNom());

        // Assigner le chef si spécifié
        if (request.getChefId() != null) {
            User chef = userRepository.findById(request.getChefId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec ID: " + request.getChefId()));

            // Vérifier que l'utilisateur a le rôle CHEF_EQUIPE
            if (!chef.estChef()) {
                throw new RuntimeException("L'utilisateur '" + chef.getNom() + "' doit avoir le rôle CHEF_EQUIPE pour être chef d'équipe");
            }

            // Vérifier si ce chef est déjà chef d'une autre équipe
            if (equipeRepository.findByChef(chef).isPresent()) {
                throw new RuntimeException("Le chef '" + chef.getNom() + "' est déjà chef d'une autre équipe");
            }

            equipe.setChef(chef);
            chef.setEquipe(equipe);
            userRepository.save(chef);
        }

        Equipe saved = equipeRepository.save(equipe);
        System.out.println("Équipe créée avec succès: " + saved.getNom());
        return new EquipeResponse(saved);
    }

    @Transactional
    public EquipeResponse updateEquipe(Long id, EquipeRequest request) {
        System.out.println("updateEquipe appelé pour ID: " + id);

        Equipe equipe = equipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Équipe non trouvée avec ID: " + id));

        // Mettre à jour le nom
        if (request.getNom() != null && !request.getNom().isEmpty()) {
            // Vérifier si le nouveau nom n'est pas déjà utilisé
            if (!equipe.getNom().equals(request.getNom()) &&
                    equipeRepository.findByNom(request.getNom()).isPresent()) {
                throw new RuntimeException("Une équipe avec le nom '" + request.getNom() + "' existe déjà");
            }
            equipe.setNom(request.getNom());
        }

        // Gérer le changement de chef
        if (request.getChefId() != null) {
            User newChef = userRepository.findById(request.getChefId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec ID: " + request.getChefId()));

            // Vérifier que l'utilisateur a le rôle CHEF_EQUIPE
            if (!newChef.estChef()) {
                throw new RuntimeException("L'utilisateur '" + newChef.getNom() + "' doit avoir le rôle CHEF_EQUIPE pour être chef d'équipe");
            }

            // Vérifier si ce chef n'est pas déjà chef d'une autre équipe (sauf s'il est déjà chef de cette équipe)
            if (equipe.getChef() == null || !equipe.getChef().getId().equals(newChef.getId())) {
                if (equipeRepository.findByChef(newChef).isPresent()) {
                    throw new RuntimeException("Le chef '" + newChef.getNom() + "' est déjà chef d'une autre équipe");
                }
            }

            // Enlever l'ancien chef s'il existe
            if (equipe.getChef() != null) {
                User oldChef = equipe.getChef();
                oldChef.setEquipe(null);
                userRepository.save(oldChef);
                System.out.println("Ancien chef retiré: " + oldChef.getNom());
            }

            // Assigner le nouveau chef
            equipe.setChef(newChef);
            newChef.setEquipe(equipe);
            userRepository.save(newChef);
            System.out.println("Nouveau chef assigné: " + newChef.getNom());
        }

        Equipe updated = equipeRepository.save(equipe);
        System.out.println("Équipe mise à jour avec succès: " + updated.getNom());
        return new EquipeResponse(updated);
    }

    @Transactional
    public void deleteEquipe(Long id) {
        System.out.println("deleteEquipe appelé pour ID: " + id);

        Equipe equipe = equipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Équipe non trouvée avec ID: " + id));

        // Dissocier tous les membres de l'équipe
        for (User membre : equipe.getMembres()) {
            membre.setEquipe(null);
            userRepository.save(membre);
            System.out.println("Membre dissocié: " + membre.getNom());
        }

        // Dissocier le chef s'il existe
        if (equipe.getChef() != null) {
            User chef = equipe.getChef();
            chef.setEquipe(null);
            userRepository.save(chef);
            System.out.println("Chef dissocié: " + chef.getNom());
        }

        equipeRepository.delete(equipe);
        System.out.println("Équipe supprimée avec succès: " + equipe.getNom());
    }
}