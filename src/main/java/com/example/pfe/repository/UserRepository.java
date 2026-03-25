package com.example.pfe.repository;



import com.example.pfe.models.Role;
import com.example.pfe.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // AJOUTE CETTE MÉTHODE
    Optional<User> findByMatricule(String matricule);

    List<User> findByRole(Role role);
    List<User> findByEquipeId(Long equipeId);
}