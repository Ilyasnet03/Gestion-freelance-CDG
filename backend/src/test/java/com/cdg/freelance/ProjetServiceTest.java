package com.cdg.freelance;

import com.cdg.freelance.dto.projet.CreateAffectationRequest;
import com.cdg.freelance.dto.projet.CreateProjetRequest;
import com.cdg.freelance.entity.Affectation;
import com.cdg.freelance.entity.Freelance;
import com.cdg.freelance.entity.Projet;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.entity.TypeProjet;
import com.cdg.freelance.entity.Utilisateur;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AffectationRepository;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.repository.LigneTimesheetRepository;
import com.cdg.freelance.repository.ProjetRepository;
import com.cdg.freelance.repository.PvReceptionRepository;
import com.cdg.freelance.repository.TacheRepository;
import com.cdg.freelance.repository.TypeProjetRepository;
import com.cdg.freelance.repository.UtilisateurRepository;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.ProjetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjetServiceTest {

    @Mock private ProjetRepository projetRepository;
    @Mock private TypeProjetRepository typeProjetRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private FreelanceRepository freelanceRepository;
    @Mock private AffectationRepository affectationRepository;
    @Mock private TacheRepository tacheRepository;
    @Mock private LigneTimesheetRepository ligneTimesheetRepository;
    @Mock private PvReceptionRepository pvReceptionRepository;
    @Mock private EntityMapper mapper;

    @Test
    void createsProjectWithTwoMonthDefaultEndDate() {
        ProjetService service = new ProjetService(
                projetRepository, typeProjetRepository, utilisateurRepository, freelanceRepository,
                affectationRepository, tacheRepository, ligneTimesheetRepository, pvReceptionRepository, mapper);

        Utilisateur manager = new Utilisateur("Chef", "Projet", "chef@cdg.ma", "hash", Role.CHEF_PROJET, true);
        manager.setId(1L);
        TypeProjet type = new TypeProjet("Web");
        type.setId(2L);

        CreateProjetRequest request = new CreateProjetRequest();
        request.setCode("PRJ-DEFAULT");
        request.setNom("Projet deux mois");
        request.setBudget(new BigDecimal("100000"));
        request.setHeuresPrevues(new BigDecimal("320"));
        request.setNombreTachesPrevues(4);
        request.setDateDebut(LocalDate.of(2026, 1, 31));
        request.setDateFinPrevue(null);
        request.setTypeProjetId(2L);
        request.setChefProjetId(1L);

        when(projetRepository.existsByCode("PRJ-DEFAULT")).thenReturn(false);
        when(typeProjetRepository.findById(2L)).thenReturn(Optional.of(type));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(projetRepository.save(any(Projet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createProjet(request, new UserPrincipal(manager));

        org.mockito.ArgumentCaptor<Projet> captor = org.mockito.ArgumentCaptor.forClass(Projet.class);
        verify(projetRepository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 3, 31), captor.getValue().getDateFinPrevue());
    }

    @Test
    void preservesExplicitProjectEndDate() {
        ProjetService service = new ProjetService(
                projetRepository, typeProjetRepository, utilisateurRepository, freelanceRepository,
                affectationRepository, tacheRepository, ligneTimesheetRepository, pvReceptionRepository, mapper);

        Utilisateur manager = new Utilisateur("Chef", "Projet", "chef@cdg.ma", "hash", Role.CHEF_PROJET, true);
        manager.setId(1L);
        TypeProjet type = new TypeProjet("Web");
        type.setId(2L);

        CreateProjetRequest request = new CreateProjetRequest();
        request.setCode("PRJ-EXPLICIT");
        request.setNom("Projet personnalisé");
        request.setBudget(new BigDecimal("100000"));
        request.setHeuresPrevues(new BigDecimal("320"));
        request.setNombreTachesPrevues(4);
        request.setDateDebut(LocalDate.of(2026, 1, 31));
        request.setDateFinPrevue(LocalDate.of(2026, 6, 15));
        request.setTypeProjetId(2L);
        request.setChefProjetId(1L);

        when(projetRepository.existsByCode("PRJ-EXPLICIT")).thenReturn(false);
        when(typeProjetRepository.findById(2L)).thenReturn(Optional.of(type));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(projetRepository.save(any(Projet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createProjet(request, new UserPrincipal(manager));

        org.mockito.ArgumentCaptor<Projet> captor = org.mockito.ArgumentCaptor.forClass(Projet.class);
        verify(projetRepository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 6, 15), captor.getValue().getDateFinPrevue());
    }

    @Test
    void rejectsFreelanceOnThirdOverlappingProject() {
        ProjetService service = new ProjetService(
                projetRepository, typeProjetRepository, utilisateurRepository, freelanceRepository,
                affectationRepository, tacheRepository, ligneTimesheetRepository, pvReceptionRepository, mapper);

        Utilisateur manager = new Utilisateur("Chef", "Projet", "chef@cdg.ma", "hash", Role.CHEF_PROJET, true);
        manager.setId(1L);
        UserPrincipal principal = new UserPrincipal(manager);

        Projet targetProject = new Projet();
        targetProject.setId(30L);
        targetProject.setNom("Projet cible");
        targetProject.setChefProjet(manager);

        Freelance freelance = new Freelance();
        freelance.setId(10L);
        freelance.setUtilisateur(new Utilisateur("Doe", "John", "john@cdg.ma", "hash", Role.FREELANCE, true));

        Projet project1 = new Projet();
        project1.setId(11L);
        Projet project2 = new Projet();
        project2.setId(12L);

        LocalDate periodStart = LocalDate.of(2026, 2, 1);
        LocalDate periodEnd = LocalDate.of(2026, 5, 31);
        Affectation aff1 = new Affectation();
        aff1.setProjet(project1);
        aff1.setDateDebut(periodStart);
        aff1.setDateFin(periodEnd);
        Affectation aff2 = new Affectation();
        aff2.setProjet(project2);
        aff2.setDateDebut(periodStart);
        aff2.setDateFin(periodEnd);

        CreateAffectationRequest request = new CreateAffectationRequest();
        request.setFreelanceId(10L);
        request.setTjmApplique(new BigDecimal("5000"));
        request.setDateDebut(LocalDate.of(2026, 3, 1));
        request.setDateFin(LocalDate.of(2026, 4, 30));

        when(projetRepository.findById(30L)).thenReturn(Optional.of(targetProject));
        when(freelanceRepository.findById(10L)).thenReturn(Optional.of(freelance));
        when(affectationRepository.findAllByFreelanceId(10L)).thenReturn(List.of(aff1, aff2));

        assertThrows(BusinessException.class,
                () -> service.addAffectation(30L, request, principal));
        verify(affectationRepository, never()).save(any());
    }
}
