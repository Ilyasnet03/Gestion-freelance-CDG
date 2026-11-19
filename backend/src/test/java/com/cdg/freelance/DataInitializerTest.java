package com.cdg.freelance;

import com.cdg.freelance.config.DataInitializer;
import com.cdg.freelance.entity.Freelance;
import com.cdg.freelance.entity.Projet;
import com.cdg.freelance.entity.TypeProjet;
import com.cdg.freelance.entity.Utilisateur;
import com.cdg.freelance.repository.AffectationRepository;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.repository.ParametreRepository;
import com.cdg.freelance.repository.ProjetRepository;
import com.cdg.freelance.repository.TacheRepository;
import com.cdg.freelance.repository.TypeProjetRepository;
import com.cdg.freelance.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private FreelanceRepository freelanceRepository;
    @Mock
    private TypeProjetRepository typeProjetRepository;
    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private AffectationRepository affectationRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private ParametreRepository parametreRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void runSeedsDemoProjectWhenUsersWerePreinitialized() {
        when(parametreRepository.count()).thenReturn(1L);
        when(typeProjetRepository.count()).thenReturn(1L);
        when(utilisateurRepository.count()).thenReturn(6L);
        when(projetRepository.count()).thenReturn(0L);

        TypeProjet typeProjet = new TypeProjet("Digital Factory - Web / Mobile");
        Utilisateur chefProjet = new Utilisateur();
        Freelance freelance = new Freelance();
        when(typeProjetRepository.findByLibelle(typeProjet.getLibelle()))
                .thenReturn(Optional.of(typeProjet));
        when(utilisateurRepository.findByEmail("cdp.alami@cdg.ma"))
                .thenReturn(Optional.of(chefProjet));
        when(freelanceRepository.findByUtilisateurEmail("mehdi.tazi@freelance.cdg.ma"))
                .thenReturn(Optional.of(freelance));
        when(projetRepository.save(any(Projet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(affectationRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        dataInitializer.run();

        ArgumentCaptor<Projet> projectCaptor = ArgumentCaptor.forClass(Projet.class);
        verify(projetRepository).save(projectCaptor.capture());
        assertEquals("PRJ-CDG-PORTAIL-2026", projectCaptor.getValue().getCode());
        verify(tacheRepository, times(2)).save(any());
    }
}
