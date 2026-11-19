package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Projet;
import com.cdg.freelance.entity.ProjetStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, Long> {
    Optional<Projet> findByCode(String code);
    boolean existsByCode(String code);
    List<Projet> findAllByChefProjetId(Long chefProjetId);
    List<Projet> findAllByStatut(ProjetStatut statut);
}
