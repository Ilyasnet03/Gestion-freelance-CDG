package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Paiement;
import com.cdg.freelance.entity.PaiementStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    Optional<Paiement> findByPvReceptionId(Long pvReceptionId);
    List<Paiement> findAllByStatut(PaiementStatut statut);
}
