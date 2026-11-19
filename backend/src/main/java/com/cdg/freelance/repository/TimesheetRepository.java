package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Timesheet;
import com.cdg.freelance.entity.TimesheetStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    Optional<Timesheet> findByFreelanceIdAndSemaineDebut(Long freelanceId, LocalDate semaineDebut);
    List<Timesheet> findAllByFreelanceId(Long freelanceId);
    List<Timesheet> findAllByStatut(TimesheetStatut statut);
    List<Timesheet> findAllBySemaineDebut(LocalDate semaineDebut);

    @Query("SELECT t FROM Timesheet t WHERE t.statut = :statut " +
           "AND t.freelance.id IN (SELECT a.freelance.id FROM Affectation a WHERE a.projet.chefProjet.id = :chefProjetId)")
    List<Timesheet> findByStatutAndChefProjetId(@Param("statut") TimesheetStatut statut, @Param("chefProjetId") Long chefProjetId);
}
