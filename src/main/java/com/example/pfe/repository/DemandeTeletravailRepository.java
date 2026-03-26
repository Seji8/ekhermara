package com.example.pfe.repository;

import com.example.pfe.models.DemandeTeletravail;
import com.example.pfe.models.StatutDemande;
import com.example.pfe.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DemandeTeletravailRepository extends JpaRepository<DemandeTeletravail, Long> {
    List<DemandeTeletravail> findByUtilisateur(User utilisateur);
    List<DemandeTeletravail> findByStatut(StatutDemande statut);
    List<DemandeTeletravail> findByUtilisateurId(Long userId);
    List<DemandeTeletravail> findByValidateurId(Long validateurId);
    List<DemandeTeletravail> findByUtilisateurIdIn(List<Long> userIds);
    List<DemandeTeletravail> findByEtapeValidationAndStatut(int etapeValidation, StatutDemande statut);
}