package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Freelance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreelanceRepository extends JpaRepository<Freelance, Long> {
    Optional<Freelance> findByUtilisateurId(Long utilisateurId);
    Optional<Freelance> findByUtilisateurEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Freelance f WHERE f.utilisateur.id = :userId")
    Optional<Freelance> findByUtilisateurIdForUpdate(@Param("userId") Long userId);
}
