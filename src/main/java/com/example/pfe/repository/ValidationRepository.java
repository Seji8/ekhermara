package com.example.pfe.repository;

import com.example.pfe.models.Validation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ValidationRepository extends JpaRepository<Validation, Long> {
    List<Validation> findByDemandeIdOrderByDateValidationAsc(Long demandeId);
}