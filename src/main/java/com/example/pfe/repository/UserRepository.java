package com.example.pfe.repository;



import com.example.pfe.models.Role;
import com.example.pfe.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);


    @Query("SELECT u FROM User u LEFT JOIN FETCH u.equipe WHERE u.id = :id")
    Optional<User> findByIdWithEquipe(@Param("id") Long id);

    //  Utilise Role enum, pas String
    List<User> findByRoleIn(List<Role> roles);
    List<User> findByEquipeId(Long equipeId);

    Optional<Object> findByMatricule(String matricule);
}