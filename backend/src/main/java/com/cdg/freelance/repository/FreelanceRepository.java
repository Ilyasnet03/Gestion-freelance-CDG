package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Freelance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreelanceRepository extends JpaRepository<Freelance, Long> {
    Optional<Freelance> findByUtilisateurId(Long utilisateurId);
    Optional<Freelance> findByUtilisateurEmail(String email);
}
