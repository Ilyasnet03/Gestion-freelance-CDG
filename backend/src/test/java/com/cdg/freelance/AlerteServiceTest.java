package com.cdg.freelance;

import com.cdg.freelance.dto.alerte.AlerteDto;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.exception.UnauthorizedException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AffectationRepository;
import com.cdg.freelance.repository.AlerteRepository;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.AlerteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlerteServiceTest {

    @Mock
    private AlerteRepository alerteRepository;

    @Mock
    private FreelanceRepository freelanceRepository;

    @Mock
    private AffectationRepository affectationRepository;

    @Mock
    private EntityMapper mapper;

    @InjectMocks
    private AlerteService alerteService;

    @Test
    @DisplayName("Un freelance ne voit que les alertes de ses projets affectés")
    void listAlertes_forFreelance_returnsOnlyAssignedProjectAlerts() {
        Utilisateur user = new Utilisateur("Rivière", "Nora", "nora.riviere@cdg.ma", "hash", Role.FREELANCE, true);
        user.setId(11L);

        Freelance freelance = new Freelance(user, new BigDecimal("550.00"), "Data", "ACTIF");
        freelance.setId(41L);

        Projet proj1 = new Projet();
        proj1.setId(101L);
        proj1.setNom("Projet A");

        Projet proj2 = new Projet();
        proj2.setId(102L);
        proj2.setNom("Projet B");

        Alerte alert1 = new Alerte();
        alert1.setId(1L);
        alert1.setProjet(proj1);
        alert1.setType(AlerteType.RETARD_PROJET);
        alert1.setNiveau(AlerteNiveau.ATTENTION);
        alert1.setMessage("Attention");
        alert1.setDateCreation(LocalDateTime.now());

        Alerte alert2 = new Alerte();
        alert2.setId(2L);
        alert2.setProjet(proj2);
        alert2.setType(AlerteType.DEPASSEMENT_BUDGET);
        alert2.setNiveau(AlerteNiveau.CRITIQUE);
        alert2.setMessage("Budget");
        alert2.setDateCreation(LocalDateTime.now());

        Affectation affectation = new Affectation();
        affectation.setId(77L);
        affectation.setProjet(proj1);
        affectation.setFreelance(freelance);

        when(freelanceRepository.findByUtilisateurId(11L)).thenReturn(Optional.of(freelance));
        when(affectationRepository.findAllByFreelanceId(41L)).thenReturn(List.of(affectation));
        when(alerteRepository.findAll()).thenReturn(List.of(alert1, alert2));
        when(mapper.toAlerteDto(any())).thenAnswer(invocation -> {
            Alerte a = invocation.getArgument(0);
            AlerteDto dto = new AlerteDto();
            dto.setId(a.getId());
            dto.setMessage(a.getMessage());
            dto.setProjetId(a.getProjet() != null ? a.getProjet().getId() : null);
            return dto;
        });

        List<AlerteDto> result = alerteService.listAlertes(new UserPrincipal(user));

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("Un freelance ne peut pas marquer comme traitée une alerte d'un autre projet")
    void markAsTraitee_forFreelance_rejectsAlertFromOtherProject() {
        Utilisateur user = new Utilisateur("Girard", "Alex", "alex.girard@cdg.ma", "hash", Role.FREELANCE, true);
        user.setId(22L);

        Freelance freelance = new Freelance(user, new BigDecimal("700.00"), "Testing", "ACTIF");
        freelance.setId(52L);

        Projet projMine = new Projet();
        projMine.setId(201L);
        projMine.setNom("Mon projet");

        Projet projAutre = new Projet();
        projAutre.setId(202L);
        projAutre.setNom("Autre projet");

        Affectation affectation = new Affectation();
        affectation.setId(88L);
        affectation.setProjet(projMine);
        affectation.setFreelance(freelance);

        Alerte alerte = new Alerte();
        alerte.setId(99L);
        alerte.setProjet(projAutre);
        alerte.setType(AlerteType.RETARD_PROJET);
        alerte.setNiveau(AlerteNiveau.CRITIQUE);
        alerte.setMessage("Autre alerte");
        alerte.setDateCreation(LocalDateTime.now());

        when(freelanceRepository.findByUtilisateurId(22L)).thenReturn(Optional.of(freelance));
        when(affectationRepository.findAllByFreelanceId(52L)).thenReturn(List.of(affectation));
        when(alerteRepository.findById(99L)).thenReturn(Optional.of(alerte));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> alerteService.markAsTraitee(99L, new UserPrincipal(user)));

        assertTrue(exception.getMessage().contains("vos projets"));
    }
}
