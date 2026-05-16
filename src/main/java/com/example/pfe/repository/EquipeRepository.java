package com.example.pfe.repository;

import com.example.pfe.models.Equipe;
import com.example.pfe.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Long> {
    Optional<Equipe> findByNom(String nom);
    Optional<Equipe> findByChef(User chef);
}