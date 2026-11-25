package com.cdg.freelance.repository;

import com.cdg.freelance.entity.PvReception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PvReceptionRepository extends JpaRepository<PvReception, Long> {
    Optional<PvReception> findByTimesheetId(Long timesheetId);
    List<PvReception> findAllByTimesheetIdIn(List<Long> timesheetIds);
    Optional<PvReception> findByNumero(String numero);
    boolean existsByTimesheetId(Long timesheetId);
}
