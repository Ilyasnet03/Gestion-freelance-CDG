package com.cdg.freelance;

import com.cdg.freelance.dto.tache.CreateTacheRequest;
import com.cdg.freelance.dto.tache.TacheDto;
import com.cdg.freelance.dto.tache.UpdateTacheRequest;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.*;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.TacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TacheServiceTest {
    @Mock private TacheRepository tacheRepository;
    @Mock private ProjetRepository projetRepository;
    @Mock private AffectationRepository affectationRepository;
    @Mock private FreelanceRepository freelanceRepository;
    @Mock private LigneTimesheetRepository ligneTimesheetRepository;
    @Mock private EntityMapper mapper;
    @InjectMocks private TacheService service;

    @Test
    void defaultHoursUsePlannedProjectHoursAndTaskCount() {
        Projet project = new Projet();
        project.setHeuresPrevues(new BigDecimal("100"));
        project.setNombreTachesPrevues(3);
        CreateTacheRequest request = new CreateTacheRequest();
        request.setTitre("Tâche");
        when(projetRepository.findById(1L)).thenReturn(Optional.of(project));
        when(tacheRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toTacheDto(any())).thenAnswer(invocation -> {
            TacheDto dto = new TacheDto();
            dto.setHeuresPrevues(invocation.<Tache>getArgument(0).getHeuresPrevues());
            return dto;
        });
        assertEquals(new BigDecimal("33.33"), service.createTache(1L, request).getHeuresPrevues());
        request.setHeuresPrevues(new BigDecimal("17.25"));
        assertEquals(new BigDecimal("17.25"), service.createTache(1L, request).getHeuresPrevues());
    }

    @Test
    void defaultRequiresConfiguredProject() {
        when(projetRepository.findById(1L)).thenReturn(Optional.of(new Projet()));
        CreateTacheRequest request = new CreateTacheRequest();
        request.setTitre("Tâche");
        assertThrows(BusinessException.class, () -> service.createTache(1L, request));
    }

    @Test
    void cannotLowerTaskQuotaBelowHoursAlreadyEntered() {
        Tache task = new Tache();
        task.setHeuresPrevues(new BigDecimal("10"));
        UpdateTacheRequest request = new UpdateTacheRequest();
        request.setTitre("Tâche");
        request.setHeuresPrevues(new BigDecimal("7.99"));
        when(tacheRepository.findForUpdateById(1L)).thenReturn(Optional.of(task));
        when(ligneTimesheetRepository.sumHeuresByTache(1L, null)).thenReturn(new BigDecimal("8"));
        assertThrows(BusinessException.class, () -> service.updateTache(1L, request));
        verify(tacheRepository, never()).save(any());
    }

    @Test
    void legacyTaskChargeIsConvertedToHours() {
        Tache task = new Tache();
        task.setChargeEstimee(new BigDecimal("1.5"));
        assertEquals(0, task.getHeuresPrevues().compareTo(new BigDecimal("12")));
    }

    @Test
    void availableMissionsExcludeTasksAtCapacityWithoutHidingOtherMissions() {
        Utilisateur user = new Utilisateur("Freelance", "CDG", "test@cdg.ma", "hash", Role.FREELANCE, true);
        user.setId(5L);
        Freelance freelance = new Freelance(user, BigDecimal.TEN, "Dev", "ACTIF");
        freelance.setId(1L);
        Tache full = new Tache();
        full.setId(10L);
        full.setHeuresPrevues(new BigDecimal("8"));
        Tache available = new Tache();
        available.setId(11L);
        available.setHeuresPrevues(new BigDecimal("9"));
        LigneTimesheetRepository.HeuresParTache total = mock(LigneTimesheetRepository.HeuresParTache.class);
        when(total.getTacheId()).thenReturn(10L);
        when(total.getHeures()).thenReturn(new BigDecimal("8"));
        when(freelanceRepository.findByUtilisateurId(5L)).thenReturn(Optional.of(freelance));
        when(tacheRepository.findAllByAffectationFreelanceId(1L)).thenReturn(List.of(full, available));
        when(ligneTimesheetRepository.sumHeuresByTacheIds(List.of(10L, 11L))).thenReturn(List.of(total));
        when(mapper.toTacheDto(any())).thenAnswer(invocation -> new EntityMapper().toTacheDto(invocation.getArgument(0)));

        assertEquals(1, service.listMesMissions(new UserPrincipal(user), true).size());
        assertEquals(2, service.listMesMissions(new UserPrincipal(user), false).size());
    }
}
