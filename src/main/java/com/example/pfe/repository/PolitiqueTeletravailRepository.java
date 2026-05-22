package com.example.pfe.repository;

import com.example.pfe.models.PolitiqueTeletravail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolitiqueTeletravailRepository extends JpaRepository<PolitiqueTeletravail, Long> {

    Optional<PolitiqueTeletravail> findByEquipeIdAndAnneeAndMois(
            Long equipeId, int annee, int mois);
}