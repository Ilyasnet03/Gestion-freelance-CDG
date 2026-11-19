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
    List<LigneTimesheet> findAllByTimesheetId(Long timesheetId);
    Optional<LigneTimesheet> findByTimesheetIdAndTacheIdAndDateSaisie(Long timesheetId, Long tacheId, LocalDate dateSaisie);

    @Query("SELECT COALESCE(SUM(l.heures), 0) FROM LigneTimesheet l " +
           "WHERE l.timesheet.freelance.id = :freelanceId AND l.dateSaisie = :dateSaisie " +
           "AND (:excludeLigneId IS NULL OR l.id != :excludeLigneId)")
    BigDecimal sumHeuresByFreelanceAndDate(
            @Param("freelanceId") Long freelanceId,
            @Param("dateSaisie") LocalDate dateSaisie,
            @Param("excludeLigneId") Long excludeLigneId);

    @Query("SELECT COALESCE(SUM(l.heures), 0) FROM LigneTimesheet l WHERE l.timesheet.id = :timesheetId")
    BigDecimal sumHeuresByTimesheetId(@Param("timesheetId") Long timesheetId);
}
