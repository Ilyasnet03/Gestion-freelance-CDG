package com.cdg.freelance;

import com.cdg.freelance.dto.alerte.AlerteDto;
import com.cdg.freelance.entity.Alerte;
import com.cdg.freelance.entity.AlerteNiveau;
import com.cdg.freelance.entity.AlerteType;
import com.cdg.freelance.entity.Projet;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.entity.Utilisateur;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AlerteRepository;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.AlerteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlerteServiceTest {

    @Mock
    private AlerteRepository alerteRepository;

    @Mock
    private EntityMapper mapper;

    @InjectMocks
    private AlerteService alerteService;

    @Test
    @DisplayName("Un chef ne voit que les alertes de ses projets")
    void listAlertes_forChef_returnsOnlyOwnedProjectAlerts() {
        Utilisateur chef = utilisateur(11L, Role.CHEF_PROJET);
        Utilisateur autreChef = utilisateur(22L, Role.CHEF_PROJET);
        Alerte ownAlert = alerte(1L, projet(101L, chef));
        Alerte otherAlert = alerte(2L, projet(102L, autreChef));

        when(alerteRepository.findAll()).thenReturn(List.of(ownAlert, otherAlert));
        when(mapper.toAlerteDto(any())).thenAnswer(invocation -> {
            Alerte source = invocation.getArgument(0);
            AlerteDto dto = new AlerteDto();
            dto.setId(source.getId());
            return dto;
        });

        List<AlerteDto> result = alerteService.listAlertes(new UserPrincipal(chef));

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("Un freelance ne peut pas consulter les alertes")
    void listAlertes_forFreelance_isDenied() {
        Utilisateur freelance = utilisateur(31L, Role.FREELANCE);

        assertThrows(AccessDeniedException.class,
                () -> alerteService.listAlertes(new UserPrincipal(freelance)));
    }

    @Test
    @DisplayName("Un chef ne peut pas traiter une alerte d'un autre projet")
    void markAsTraitee_forOtherProject_isDenied() {
        Utilisateur chef = utilisateur(11L, Role.CHEF_PROJET);
        Utilisateur autreChef = utilisateur(22L, Role.CHEF_PROJET);
        Alerte alert = alerte(9L, projet(102L, autreChef));
        when(alerteRepository.findById(9L)).thenReturn(java.util.Optional.of(alert));

        assertThrows(AccessDeniedException.class,
                () -> alerteService.markAsTraitee(9L, new UserPrincipal(chef)));
    }

    private Utilisateur utilisateur(Long id, Role role) {
        Utilisateur user = new Utilisateur("Test", "User", "user" + id + "@cdg.ma", "hash", role, true);
        user.setId(id);
        return user;
    }

    private Projet projet(Long id, Utilisateur chef) {
        Projet project = new Projet();
        project.setId(id);
        project.setChefProjet(chef);
        return project;
    }

    private Alerte alerte(Long id, Projet project) {
        Alerte alert = new Alerte();
        alert.setId(id);
        alert.setProjet(project);
        alert.setType(AlerteType.RETARD_PROJET);
        alert.setNiveau(AlerteNiveau.ATTENTION);
        alert.setMessage("Test alert");
        alert.setDateCreation(LocalDateTime.now());
        return alert;
    }
}
