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
    }

    // ===================== GET ALL =====================
    public List<EquipeResponse> getAllEquipes() {

        return equipeRepository.findAll().stream()
                .map(equipe -> {

                    int nombreMembres = userRepository.countByEquipeId(equipe.getId());

                    return new EquipeResponse(equipe, nombreMembres);
                })
                .collect(Collectors.toList());
    }

    // ===================== GET BY ID =====================
    public EquipeResponse getEquipeById(Long id) {

        Equipe equipe = equipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Équipe non trouvée"));

        int nombreMembres = userRepository.countByEquipeId(equipe.getId());

        return new EquipeResponse(equipe, nombreMembres);
    }

    // ===================== CREATE =====================
    @Transactional
    public EquipeResponse createEquipe(EquipeRequest request) {

        if (equipeRepository.findByNom(request.getNom()).isPresent()) {
            throw new RuntimeException("Une équipe avec ce nom existe déjà");
        }

        Equipe equipe = new Equipe();
        equipe.setNom(request.getNom());

        if (request.getChefId() != null) {

            User chef = userRepository.findById(request.getChefId())
                    .orElseThrow(() -> new RuntimeException("Chef introuvable"));

            if (!chef.estChef()) {
                throw new RuntimeException("Utilisateur doit être CHEF_EQUIPE");
            }

            if (equipeRepository.findByChef(chef).isPresent()) {
                throw new RuntimeException("Ce chef gère déjà une équipe");
            }

            equipe.setChef(chef);
            chef.setEquipe(equipe);
            userRepository.save(chef);
        }

        Equipe saved = equipeRepository.save(equipe);

        int nombreMembres = 0;

        return new EquipeResponse(saved, nombreMembres);
    }

    // ===================== UPDATE =====================
    @Transactional
    public EquipeResponse updateEquipe(Long id, EquipeRequest request) {

        Equipe equipe = equipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Équipe introuvable"));

        if (request.getNom() != null && !request.getNom().isEmpty()) {

            if (!equipe.getNom().equals(request.getNom())
                    && equipeRepository.findByNom(request.getNom()).isPresent()) {
                throw new RuntimeException("Nom déjà utilisé");
            }

            equipe.setNom(request.getNom());
        }

        if (request.getChefId() != null) {

            User newChef = userRepository.findById(request.getChefId())
                    .orElseThrow(() -> new RuntimeException("Chef introuvable"));

            if (!newChef.estChef()) {
                throw new RuntimeException("Utilisateur doit être CHEF_EQUIPE");
            }

            if (equipeRepository.findByChef(newChef).isPresent()
                    && (equipe.getChef() == null || !equipe.getChef().getId().equals(newChef.getId()))) {
                throw new RuntimeException("Chef déjà assigné à une autre équipe");
            }

            if (equipe.getChef() != null) {
                User oldChef = equipe.getChef();
                oldChef.setEquipe(null);
                userRepository.save(oldChef);
            }

            equipe.setChef(newChef);
            newChef.setEquipe(equipe);
            userRepository.save(newChef);
        }

        Equipe updated = equipeRepository.save(equipe);

        int nombreMembres = userRepository.countByEquipeId(updated.getId());

        return new EquipeResponse(updated, nombreMembres);
    }

    // ===================== DELETE =====================
    @Transactional
    public void deleteEquipe(Long id) {

        Equipe equipe = equipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Équipe introuvable"));

        // 🔥 FIX IMPORTANT : ne PAS utiliser getMembres()

        List<User> membres = userRepository.findByEquipeId(id);

        for (User membre : membres) {
            membre.setEquipe(null);
            userRepository.save(membre);
        }

        if (equipe.getChef() != null) {
            User chef = equipe.getChef();
            chef.setEquipe(null);
            userRepository.save(chef);
        }

        equipeRepository.delete(equipe);
    }
}