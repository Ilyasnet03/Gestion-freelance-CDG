package com.cdg.freelance.repository;

import com.cdg.freelance.entity.PvReception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PvReceptionRepository extends JpaRepository<PvReception, Long> {
    Optional<PvReception> findByTimesheetId(Long timesheetId);
    Optional<PvReception> findByNumero(String numero);
    boolean existsByTimesheetId(Long timesheetId);
}
