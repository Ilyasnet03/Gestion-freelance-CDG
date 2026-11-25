package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TacheRepository extends JpaRepository<Tache, Long> {
    List<Tache> findAllByProjetId(Long projetId);
    List<Tache> findAllByAffectationId(Long affectationId);
    List<Tache> findAllByAffectationFreelanceId(Long freelanceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tache t WHERE t.id = :id")
    Optional<Tache> findForUpdateById(@Param("id") Long id);
}
