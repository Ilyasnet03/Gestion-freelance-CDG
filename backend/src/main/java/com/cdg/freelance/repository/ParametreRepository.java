package com.cdg.freelance.repository;

import com.cdg.freelance.entity.Parametre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametreRepository extends JpaRepository<Parametre, String> {
    Optional<Parametre> findByCle(String cle);
}
