package com.cdg.freelance;

import com.cdg.freelance.dto.freelance.CreateFreelanceRequest;
import com.cdg.freelance.dto.freelance.FreelanceDto;
import com.cdg.freelance.entity.Freelance;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.entity.Utilisateur;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.repository.UtilisateurRepository;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.FreelanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreelanceServiceTest {

    @Mock
    private FreelanceRepository freelanceRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private EntityMapper mapper;

    @InjectMocks
    private FreelanceService freelanceService;

    @Test
    @DisplayName("Retourne la fiche freelance d'un utilisateur via son identifiant")
    void getFreelanceByUserId_returnsMappedDto() {
        Utilisateur user = new Utilisateur("Tazi", "Mehdi", "mehdi.tazi@cdg.ma", "hash", Role.FREELANCE, true);
        user.setId(7L);

        Freelance freelance = new Freelance(user, new BigDecimal("550.00"), "Java", "ACTIF");
        freelance.setId(33L);

        FreelanceDto expected = new FreelanceDto();
        expected.setId(33L);
        expected.setUtilisateurId(7L);
        expected.setTjm(new BigDecimal("550.00"));
        expected.setSpecialite("Java");
        expected.setStatut("ACTIF");

        when(freelanceRepository.findByUtilisateurId(7L)).thenReturn(Optional.of(freelance));
        when(mapper.toFreelanceDto(freelance)).thenReturn(expected);

        FreelanceDto actual = freelanceService.getFreelanceByUserId(7L, new UserPrincipal(user));

        assertSame(expected, actual);
        assertEquals(7L, actual.getUtilisateurId());
        assertEquals("Java", actual.getSpecialite());
    }

    @Test
    @DisplayName("Une fiche freelance ne peut pas être créée deux fois pour le même utilisateur")
    void createFreelance_throwsWhenUserAlreadyHasProfile() {
        Utilisateur user = new Utilisateur("Martin", "Sophie", "sophie.martin@cdg.ma", "hash", Role.FREELANCE, true);
        user.setId(15L);

        CreateFreelanceRequest request = new CreateFreelanceRequest();
        request.setUtilisateurId(15L);
        request.setTjm(new BigDecimal("600.00"));
        request.setSpecialite("Data");
        request.setStatut("ACTIF");

        when(utilisateurRepository.findById(15L)).thenReturn(Optional.of(user));
        when(freelanceRepository.findByUtilisateurId(15L)).thenReturn(Optional.of(new Freelance(user, new BigDecimal("500.00"), "Dev", "ACTIF")));

        BusinessException exception = assertThrows(BusinessException.class, () -> freelanceService.createFreelance(request));

        assertTrue(exception.getMessage().contains("déjà"));
    }
}
