package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AffectationRepository extends JpaRepository<Affectation, Long> {
    List<Affectation> findAllByProjetId(Long projetId);
    List<Affectation> findAllByFreelanceId(Long freelanceId);
    Optional<Affectation> findByProjetIdAndFreelanceId(Long projetId, Long freelanceId);

    @Query("SELECT a FROM Affectation a WHERE a.freelance.id = :freelanceId " +
           "AND a.dateDebut <= :date AND (a.dateFin IS NULL OR a.dateFin >= :date)")
    List<Affectation> findActiveAffectationsByFreelanceAndDate(
            @Param("freelanceId") Long freelanceId,
            @Param("date") LocalDate date);
}
