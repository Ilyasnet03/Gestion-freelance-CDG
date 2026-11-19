package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Alerte;
import com.cdg.freelance.entity.AlerteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlerteRepository extends JpaRepository<Alerte, Long> {
    List<Alerte> findAllByProjetId(Long projetId);
    List<Alerte> findAllByTraitee(Boolean traitee);
    List<Alerte> findAllByProjetChefProjetIdAndTraitee(Long chefProjetId, Boolean traitee);
    Optional<Alerte> findByProjetIdAndTypeAndTraitee(Long projetId, AlerteType type, Boolean traitee);
}
