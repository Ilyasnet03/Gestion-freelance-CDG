package com.cdg.freelance;

import com.cdg.freelance.dto.timesheet.AddLigneRequest;
import com.cdg.freelance.dto.timesheet.TimesheetDto;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.*;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.PvService;
import com.cdg.freelance.service.TimesheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TimesheetServiceTest {

    @Mock private TimesheetRepository timesheetRepository;
    @Mock private LigneTimesheetRepository ligneTimesheetRepository;
    @Mock private FreelanceRepository freelanceRepository;
    @Mock private TacheRepository tacheRepository;
    @Mock private AffectationRepository affectationRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PvReceptionRepository pvReceptionRepository;
    @Mock private PvService pvService;
    @Mock private EntityMapper mapper;

    @InjectMocks
    private TimesheetService timesheetService;

    private Utilisateur freelanceUser;
    private Freelance freelance;
    private UserPrincipal userPrincipal;
    private Projet projet;
    private Affectation affectation;
    private Tache tache;
    private Timesheet timesheet;

    @BeforeEach
    void setUp() {
        freelanceUser = new Utilisateur("TAZI", "Mehdi", "mehdi.tazi@freelance.cdg.ma", "hash", Role.FREELANCE, true);
        freelanceUser.setId(10L);

        freelance = new Freelance(freelanceUser, new BigDecimal("5000.00"), "Dev Fullstack", "ACTIF");
        freelance.setId(1L);

        userPrincipal = new UserPrincipal(freelanceUser);

        projet = new Projet();
        projet.setId(100L);
        projet.setCode("PRJ-01");
        projet.setNom("Portail CDG");

        affectation = new Affectation();
        affectation.setId(200L);
        affectation.setProjet(projet);
        affectation.setFreelance(freelance);
        affectation.setTjmApplique(new BigDecimal("5000.00"));
        affectation.setDateDebut(LocalDate.now().minusWeeks(2));
        affectation.setDateFin(LocalDate.now().plusWeeks(10));

        tache = new Tache();
        tache.setId(300L);
        tache.setProjet(projet);
        tache.setAffectation(affectation);
        tache.setTitre("Dev Frontend");

        timesheet = new Timesheet();
        timesheet.setId(500L);
        timesheet.setFreelance(freelance);
        // Find a recent Monday
        LocalDate monday = LocalDate.now();
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.minusDays(1);
        }
        timesheet.setSemaineDebut(monday);
        timesheet.setStatut(TimesheetStatut.BROUILLON);
    }

    @Test
    @DisplayName("RG1: Une semaine_debut de timesheet doit obligatoirement être un lundi")
    void testOpenTimesheet_MustBeMonday() {
        when(freelanceRepository.findByUtilisateurId(freelanceUser.getId())).thenReturn(Optional.of(freelance));

        // Tuesday
        LocalDate tuesday = LocalDate.of(2026, 12, 8); // Tuesday
        assertThrows(BusinessException.class, () -> {
            timesheetService.openOrCreateTimesheet(tuesday, userPrincipal);
        });
    }

    @Test
    @DisplayName("RG4: Le total d'heures d'une journée ne peut pas dépasser 24h")
    void testAddLigne_Max24hPerDay() {
        when(timesheetRepository.findById(500L)).thenReturn(Optional.of(timesheet));
        when(freelanceRepository.findByUtilisateurId(freelanceUser.getId())).thenReturn(Optional.of(freelance));
        when(tacheRepository.findById(300L)).thenReturn(Optional.of(tache));

        LocalDate saisieDate = timesheet.getSemaineDebut();
        when(affectationRepository.findActiveAffectationsByFreelanceAndDate(eq(1L), eq(saisieDate)))
                .thenReturn(List.of(affectation));

        // Already 18 hours logged today
        when(ligneTimesheetRepository.sumHeuresByFreelanceAndDate(eq(1L), eq(saisieDate), any()))
                .thenReturn(new BigDecimal("18.00"));

        // Freelance attempts to add 8 hours more -> total 26h > 24h
        AddLigneRequest request = new AddLigneRequest(300L, saisieDate, new BigDecimal("8.00"));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            timesheetService.addOrUpdateLigne(500L, request, userPrincipal);
        });

        assertTrue(exception.getMessage().contains("24 h"));
    }

    @Test
    @DisplayName("RG6: Une timesheet VALIDEE ne peut plus être modifiée")
    void testAddLigne_CannotModifyValidatedTimesheet() {
        timesheet.setStatut(TimesheetStatut.VALIDEE);
        when(timesheetRepository.findById(500L)).thenReturn(Optional.of(timesheet));
        when(freelanceRepository.findByUtilisateurId(freelanceUser.getId())).thenReturn(Optional.of(freelance));

        AddLigneRequest request = new AddLigneRequest(300L, timesheet.getSemaineDebut(), new BigDecimal("4.00"));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            timesheetService.addOrUpdateLigne(500L, request, userPrincipal);
        });

        assertTrue(exception.getMessage().contains("verrouillée"));
    }
}
