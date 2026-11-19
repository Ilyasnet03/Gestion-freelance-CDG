package com.cdg.freelance;

import com.cdg.freelance.dto.facturation.PaiementDto;
import com.cdg.freelance.dto.facturation.UpdatePaiementRequest;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AffectationRepository;
import com.cdg.freelance.repository.LigneTimesheetRepository;
import com.cdg.freelance.repository.PaiementRepository;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.PaiementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    @Mock private PaiementRepository paiementRepository;
    @Mock private LigneTimesheetRepository ligneTimesheetRepository;
    @Mock private AffectationRepository affectationRepository;
    @Mock private EntityMapper mapper;
    @InjectMocks private PaiementService paiementService;

    @Test
    void chefCannotConfirmPaymentForTimesheetSpanningAnotherProject() {
        Utilisateur chef = user(11L);
        Paiement paiement = payment(timesheetLines(chef, user(22L)));
        when(paiementRepository.findById(7L)).thenReturn(Optional.of(paiement));
        when(ligneTimesheetRepository.findAllByTimesheetId(50L))
                .thenReturn(paiement.getPvReception().getTimesheet().getLignes());

        assertThrows(AccessDeniedException.class, () -> paiementService.updatePaiement(
                7L,
                new UpdatePaiementRequest(PaiementStatut.PAYE, LocalDate.now(), "R-1"),
                new UserPrincipal(chef)));
        verify(paiementRepository, never()).save(any());
    }

    @Test
    void chefCanConfirmPaymentForTheirOwnProject() {
        Utilisateur chef = user(11L);
        Paiement paiement = payment(timesheetLines(chef));
        when(paiementRepository.findById(7L)).thenReturn(Optional.of(paiement));
        when(ligneTimesheetRepository.findAllByTimesheetId(50L))
                .thenReturn(paiement.getPvReception().getTimesheet().getLignes());
        when(paiementRepository.save(paiement)).thenReturn(paiement);
        when(mapper.toPaiementDto(paiement)).thenReturn(new PaiementDto());

        paiementService.updatePaiement(
                7L,
                new UpdatePaiementRequest(PaiementStatut.PAYE, LocalDate.now(), "R-1"),
                new UserPrincipal(chef));

        assertEquals(PaiementStatut.PAYE, paiement.getStatut());
        assertEquals("R-1", paiement.getReference());
    }

    private Utilisateur user(Long id) {
        Utilisateur user = new Utilisateur("Chef", "CDG", "chef" + id + "@cdg.ma", "hash", Role.CHEF_PROJET, true);
        user.setId(id);
        return user;
    }

    private List<LigneTimesheet> timesheetLines(Utilisateur... chefs) {
        Timesheet timesheet = new Timesheet();
        timesheet.setId(50L);
        timesheet.setFreelance(new Freelance());
        timesheet.setSemaineDebut(LocalDate.now());

        List<LigneTimesheet> lines = new java.util.ArrayList<>();
        for (int index = 0; index < chefs.length; index++) {
            Projet project = new Projet();
            project.setId((long) index + 100);
            project.setChefProjet(chefs[index]);
            Tache task = new Tache();
            task.setProjet(project);
            LigneTimesheet line = new LigneTimesheet();
            line.setTimesheet(timesheet);
            line.setTache(task);
            lines.add(line);
        }
        timesheet.setLignes(lines);
        return lines;
    }

    private Paiement payment(List<LigneTimesheet> lines) {
        Timesheet timesheet = lines.get(0).getTimesheet();
        PvReception pv = new PvReception();
        pv.setId(60L);
        pv.setTimesheet(timesheet);
        Paiement paiement = new Paiement();
        paiement.setId(7L);
        paiement.setPvReception(pv);
        paiement.setStatut(PaiementStatut.EN_ATTENTE);
        return paiement;
    }
}
