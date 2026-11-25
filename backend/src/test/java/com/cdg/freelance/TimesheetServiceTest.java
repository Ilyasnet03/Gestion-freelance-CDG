package com.cdg.freelance;

import com.cdg.freelance.dto.timesheet.AddLigneRequest;
import com.cdg.freelance.dto.timesheet.TimesheetDto;
import com.cdg.freelance.dto.timesheet.LigneTimesheetDto;
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
        tache.setChargeEstimee(new BigDecimal("10.0"));

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
    @DisplayName("Une soumission sans confirmation explicite est refusée")
    void testSoumettreTimesheet_RequiresExplicitConfirmation() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                timesheetService.soumettreTimesheet(500L, false, userPrincipal));

        assertTrue(exception.getMessage().contains("confirmation explicite"));
        verifyNoInteractions(timesheetRepository);
    }

    @Test
    @DisplayName("Le total journalier ne peut pas dépasser 10 h")
    void testAddLigne_Max10hPerDay() {
        when(timesheetRepository.findById(500L)).thenReturn(Optional.of(timesheet));
        when(freelanceRepository.findByUtilisateurIdForUpdate(freelanceUser.getId())).thenReturn(Optional.of(freelance));
        when(tacheRepository.findForUpdateById(300L)).thenReturn(Optional.of(tache));

        LocalDate saisieDate = timesheet.getSemaineDebut();
        when(affectationRepository.findActiveAffectationsByFreelanceAndDate(eq(1L), eq(saisieDate)))
                .thenReturn(List.of(affectation));

        when(ligneTimesheetRepository.sumHeuresByFreelanceAndDate(eq(1L), eq(saisieDate), any()))
                .thenReturn(new BigDecimal("2.01"));

        AddLigneRequest request = new AddLigneRequest(300L, saisieDate, new BigDecimal("8.00"));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            timesheetService.addOrUpdateLigne(500L, request, userPrincipal);
        });

        assertTrue(exception.getMessage().contains("10 h"));
        verify(ligneTimesheetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une saisie au-dessus du seuil individuel reste possible jusqu'à 10 h et avertit")
    void testAddLigne_WarnsAboveIndividualThreshold() {
        when(timesheetRepository.findById(500L)).thenReturn(Optional.of(timesheet));
        when(freelanceRepository.findByUtilisateurIdForUpdate(freelanceUser.getId())).thenReturn(Optional.of(freelance));
        when(tacheRepository.findForUpdateById(300L)).thenReturn(Optional.of(tache));
        LocalDate day = timesheet.getSemaineDebut();
        when(affectationRepository.findActiveAffectationsByFreelanceAndDate(1L, day)).thenReturn(List.of(affectation));
        when(ligneTimesheetRepository.sumHeuresByFreelanceAndDate(eq(1L), eq(day), any())).thenReturn(new BigDecimal("2"));
        when(ligneTimesheetRepository.sumHeuresByTache(300L, null)).thenReturn(BigDecimal.ZERO);
        when(ligneTimesheetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toLigneTimesheetDto(any())).thenAnswer(invocation -> new LigneTimesheetDto());

        LigneTimesheetDto result = timesheetService.addOrUpdateLigne(500L,
                new AddLigneRequest(300L, day, new BigDecimal("8")), userPrincipal);
        assertTrue(result.getAvertissement().contains("10 h"));
        assertTrue(result.getAvertissement().contains("8 h"));
    }

    @Test
    @DisplayName("Une modification remplace la ligne existante et respecte le seuil configuré")
    void testAddLigne_ExcludesExistingLineAndUsesConfiguredThreshold() {
        freelance.setSeuilHeuresJour(new BigDecimal("9"));
        LocalDate day = timesheet.getSemaineDebut();
        LigneTimesheet existing = new LigneTimesheet(timesheet, tache, day, new BigDecimal("5"));
        existing.setId(7L);
        when(timesheetRepository.findById(500L)).thenReturn(Optional.of(timesheet));
        when(freelanceRepository.findByUtilisateurIdForUpdate(freelanceUser.getId())).thenReturn(Optional.of(freelance));
        when(tacheRepository.findForUpdateById(300L)).thenReturn(Optional.of(tache));
        when(affectationRepository.findActiveAffectationsByFreelanceAndDate(1L, day)).thenReturn(List.of(affectation));
        when(ligneTimesheetRepository.findByTimesheetIdAndTacheIdAndDateSaisie(500L, 300L, day)).thenReturn(Optional.of(existing));
        when(ligneTimesheetRepository.sumHeuresByFreelanceAndDate(1L, day, 7L)).thenReturn(new BigDecimal("1"));
        when(ligneTimesheetRepository.sumHeuresByTache(300L, 7L)).thenReturn(new BigDecimal("72"));
        when(ligneTimesheetRepository.save(existing)).thenReturn(existing);
        when(mapper.toLigneTimesheetDto(existing)).thenReturn(new LigneTimesheetDto());

        LigneTimesheetDto result = timesheetService.addOrUpdateLigne(500L,
                new AddLigneRequest(300L, day, new BigDecimal("8")), userPrincipal);
        assertNull(result.getAvertissement());
        assertEquals(new BigDecimal("8"), existing.getHeures());
    }

    @Test
    @DisplayName("Les saisies dépassant le plafond de la tâche sont refusées")
    void testAddLigne_RejectsTaskOverrun() {
        LocalDate day = timesheet.getSemaineDebut();
        tache.setHeuresPrevues(new BigDecimal("10.00"));
        when(timesheetRepository.findById(500L)).thenReturn(Optional.of(timesheet));
        when(freelanceRepository.findByUtilisateurIdForUpdate(freelanceUser.getId())).thenReturn(Optional.of(freelance));
        when(tacheRepository.findForUpdateById(300L)).thenReturn(Optional.of(tache));
        when(affectationRepository.findActiveAffectationsByFreelanceAndDate(1L, day)).thenReturn(List.of(affectation));
        when(ligneTimesheetRepository.sumHeuresByFreelanceAndDate(1L, day, null)).thenReturn(BigDecimal.ZERO);
        when(ligneTimesheetRepository.sumHeuresByTache(300L, null)).thenReturn(new BigDecimal("8.50"));

        BusinessException exception = assertThrows(BusinessException.class, () -> timesheetService.addOrUpdateLigne(
                500L, new AddLigneRequest(300L, day, new BigDecimal("1.51")), userPrincipal));
        assertTrue(exception.getMessage().contains("heures prévues"));
        verify(ligneTimesheetRepository, never()).save(any());
    }

    @Test
    @DisplayName("RG6: Une timesheet VALIDEE ne peut plus être modifiée")
    void testAddLigne_CannotModifyValidatedTimesheet() {
        timesheet.setStatut(TimesheetStatut.VALIDEE);
        when(timesheetRepository.findById(500L)).thenReturn(Optional.of(timesheet));
        when(freelanceRepository.findByUtilisateurIdForUpdate(freelanceUser.getId())).thenReturn(Optional.of(freelance));

        AddLigneRequest request = new AddLigneRequest(300L, timesheet.getSemaineDebut(), new BigDecimal("4.00"));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            timesheetService.addOrUpdateLigne(500L, request, userPrincipal);
        });

        assertTrue(exception.getMessage().contains("verrouillée"));
    }

    @Test
    @DisplayName("La validation d'une timesheet met à jour l'avancement de la tâche")
    void testValiderTimesheet_UpdatesTaskProgress() {
        Utilisateur chef = new Utilisateur("Chef", "CDG", "chef@cdg.ma", "hash", Role.CHEF_PROJET, true);
        chef.setId(20L);
        projet.setChefProjet(chef);
        tache.setChargeEstimee(new BigDecimal("10.0"));

        LigneTimesheet ligne = new LigneTimesheet(timesheet, tache,
                timesheet.getSemaineDebut(), new BigDecimal("40.00"));
        ligne.setId(400L);
        timesheet.setStatut(TimesheetStatut.SOUMISE);

        UserPrincipal chefPrincipal = new UserPrincipal(chef);

        when(timesheetRepository.findById(500L)).thenReturn(Optional.of(timesheet));
        when(ligneTimesheetRepository.findAllByTimesheetId(500L)).thenReturn(List.of(ligne));
        when(ligneTimesheetRepository.findAllByTacheId(300L)).thenReturn(List.of(ligne));
        when(utilisateurRepository.findById(20L)).thenReturn(Optional.of(chef));
        when(timesheetRepository.save(timesheet)).thenReturn(timesheet);
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(mapper.toTimesheetDto(timesheet)).thenReturn(new TimesheetDto());
        when(pvReceptionRepository.findByTimesheetId(500L)).thenReturn(Optional.empty());

        timesheetService.validerTimesheet(500L, chefPrincipal);

        assertEquals(50, tache.getAvancement());
        assertEquals(TacheStatut.EN_COURS, tache.getStatut());
        verify(tacheRepository).save(tache);
    }

    @Test
    @DisplayName("Une validation ne fait jamais diminuer un avancement déjà supérieur")
    void testValiderTimesheet_DoesNotDecreaseTaskProgress() {
        Utilisateur chef = new Utilisateur("Chef", "CDG", "chef@cdg.ma", "hash", Role.CHEF_PROJET, true);
        chef.setId(20L);
        projet.setChefProjet(chef);
        tache.setChargeEstimee(new BigDecimal("10.0"));
        tache.setAvancement(75);

        LigneTimesheet ligne = new LigneTimesheet(timesheet, tache,
                timesheet.getSemaineDebut(), new BigDecimal("40.00"));
        ligne.setId(400L);
        timesheet.setStatut(TimesheetStatut.SOUMISE);

        UserPrincipal chefPrincipal = new UserPrincipal(chef);

        when(timesheetRepository.findById(500L)).thenReturn(Optional.of(timesheet));
        when(ligneTimesheetRepository.findAllByTimesheetId(500L)).thenReturn(List.of(ligne));
        when(ligneTimesheetRepository.findAllByTacheId(300L)).thenReturn(List.of(ligne));
        when(utilisateurRepository.findById(20L)).thenReturn(Optional.of(chef));
        when(timesheetRepository.save(timesheet)).thenReturn(timesheet);
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(mapper.toTimesheetDto(timesheet)).thenReturn(new TimesheetDto());
        when(pvReceptionRepository.findByTimesheetId(500L)).thenReturn(Optional.empty());

        timesheetService.validerTimesheet(500L, chefPrincipal);

        assertEquals(75, tache.getAvancement());
        assertEquals(TacheStatut.EN_COURS, tache.getStatut());
    }

    @Test
    @DisplayName("Le filtrage par statut ne retourne que les timesheets correspondant à l'état demandé")
    void testListTimesheets_FiltersByStatus() {
        Timesheet soumise = new Timesheet();
        soumise.setId(501L);
        soumise.setFreelance(freelance);
        soumise.setSemaineDebut(timesheet.getSemaineDebut().plusWeeks(1));
        soumise.setStatut(TimesheetStatut.SOUMISE);

        when(freelanceRepository.findByUtilisateurId(freelanceUser.getId()))
                .thenReturn(Optional.of(freelance));
        when(timesheetRepository.findAllByFreelanceId(freelance.getId()))
                .thenReturn(List.of(timesheet, soumise));
        when(ligneTimesheetRepository.findAllByTimesheetIdIn(anyList()))
                .thenReturn(List.of());
        when(pvReceptionRepository.findAllByTimesheetIdIn(anyList()))
                .thenReturn(List.of());
        when(mapper.toTimesheetDto(any(Timesheet.class)))
                .thenAnswer(invocation -> {
                    TimesheetDto dto = new TimesheetDto();
                    dto.setId(invocation.<Timesheet>getArgument(0).getId());
                    return dto;
                });

        List<TimesheetDto> result =
                timesheetService.listTimesheets(null, TimesheetStatut.SOUMISE, userPrincipal);

        assertEquals(1, result.size());
        assertEquals(501L, result.get(0).getId());
    }

    @Test
    @DisplayName("Un chef ne voit pas une timesheet couvrant aussi le projet d'un autre chef")
    void testListTimesheets_HidesMixedProjectTimesheetFromChef() {
        Utilisateur chef = new Utilisateur("Chef", "CDG", "chef@cdg.ma", "hash", Role.CHEF_PROJET, true);
        chef.setId(20L);
        Utilisateur autreChef = new Utilisateur("Autre", "Chef", "autre@cdg.ma", "hash", Role.CHEF_PROJET, true);
        autreChef.setId(21L);

        Projet projetDuChef = new Projet();
        projetDuChef.setId(101L);
        projetDuChef.setChefProjet(chef);
        tache.setProjet(projetDuChef);

        Projet projetAutreChef = new Projet();
        projetAutreChef.setId(102L);
        projetAutreChef.setChefProjet(autreChef);
        Tache autreTache = new Tache();
        autreTache.setProjet(projetAutreChef);

        LigneTimesheet ligneChef = new LigneTimesheet();
        ligneChef.setTimesheet(timesheet);
        ligneChef.setTache(tache);
        LigneTimesheet ligneAutreChef = new LigneTimesheet();
        ligneAutreChef.setTimesheet(timesheet);
        ligneAutreChef.setTache(autreTache);

        when(timesheetRepository.findAll()).thenReturn(List.of(timesheet));
        when(ligneTimesheetRepository.findAllByTimesheetIdIn(List.of(timesheet.getId())))
                .thenReturn(List.of(ligneChef, ligneAutreChef));

        List<TimesheetDto> result = timesheetService.listTimesheets(null, new UserPrincipal(chef));

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }
}
