package com.cdg.freelance.repository;

import com.cdg.freelance.entity.LigneTimesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LigneTimesheetRepository extends JpaRepository<LigneTimesheet, Long> {
    interface HeuresParTache {
        Long getTacheId();
        BigDecimal getHeures();
    }

    List<LigneTimesheet> findAllByTimesheetId(Long timesheetId);
    List<LigneTimesheet> findAllByTimesheetIdIn(List<Long> timesheetIds);
    List<LigneTimesheet> findAllByTacheId(Long tacheId);
    Optional<LigneTimesheet> findByTimesheetIdAndTacheIdAndDateSaisie(Long timesheetId, Long tacheId, LocalDate dateSaisie);

    @Query("SELECT COALESCE(SUM(l.heures), 0) FROM LigneTimesheet l " +
           "WHERE l.timesheet.freelance.id = :freelanceId AND l.dateSaisie = :dateSaisie " +
           "AND (:excludeLigneId IS NULL OR l.id != :excludeLigneId)")
    BigDecimal sumHeuresByFreelanceAndDate(
            @Param("freelanceId") Long freelanceId,
            @Param("dateSaisie") LocalDate dateSaisie,
            @Param("excludeLigneId") Long excludeLigneId);

    @Query("SELECT COALESCE(SUM(l.heures), 0) FROM LigneTimesheet l " +
           "WHERE l.tache.id = :tacheId AND l.id <> COALESCE(:excludeLigneId, -1)")
    BigDecimal sumHeuresByTache(@Param("tacheId") Long tacheId,
                                @Param("excludeLigneId") Long excludeLigneId);

    @Query("SELECT l.tache.id AS tacheId, SUM(l.heures) AS heures FROM LigneTimesheet l " +
           "WHERE l.tache.id IN :ids GROUP BY l.tache.id")
    List<HeuresParTache> sumHeuresByTacheIds(@Param("ids") List<Long> ids);

    @Query("SELECT COALESCE(SUM(l.heures), 0) FROM LigneTimesheet l WHERE l.timesheet.id = :timesheetId")
    BigDecimal sumHeuresByTimesheetId(@Param("timesheetId") Long timesheetId);
}
