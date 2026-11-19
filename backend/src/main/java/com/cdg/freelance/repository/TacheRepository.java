package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TacheRepository extends JpaRepository<Tache, Long> {
    List<Tache> findAllByProjetId(Long projetId);
    List<Tache> findAllByAffectationId(Long affectationId);
}
