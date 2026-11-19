package com.cdg.freelance.repository;

import com.cdg.freelance.entity.TypeProjet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeProjetRepository extends JpaRepository<TypeProjet, Long> {
    Optional<TypeProjet> findByLibelle(String libelle);
    boolean existsByLibelle(String libelle);
}
