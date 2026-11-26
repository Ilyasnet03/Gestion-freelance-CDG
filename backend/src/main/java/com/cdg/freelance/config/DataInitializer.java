package com.cdg.freelance.config;

import com.cdg.freelance.entity.*;
import com.cdg.freelance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Profile("!prod & !production")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UtilisateurRepository utilisateurRepository;
    private final FreelanceRepository freelanceRepository;
    private final TypeProjetRepository typeProjetRepository;
    private final ProjetRepository projetRepository;
    private final AffectationRepository affectationRepository;
    private final TacheRepository tacheRepository;
    private final ParametreRepository parametreRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UtilisateurRepository utilisateurRepository,
                           FreelanceRepository freelanceRepository,
                           TypeProjetRepository typeProjetRepository,
                           ProjetRepository projetRepository,
                           AffectationRepository affectationRepository,
                           TacheRepository tacheRepository,
                           ParametreRepository parametreRepository,
                           PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.freelanceRepository = freelanceRepository;
        this.typeProjetRepository = typeProjetRepository;
        this.projetRepository = projetRepository;
        this.affectationRepository = affectationRepository;
        this.tacheRepository = tacheRepository;
        this.parametreRepository = parametreRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Initialiser les paramètres système
        if (parametreRepository.count() == 0) {
            parametreRepository.saveAll(List.of(
                    new Parametre("TJM_DEFAUT", "4500"),
                    new Parametre("SEUIL_BUDGET_ATTENTION", "80"),
                    new Parametre("SEUIL_BUDGET_CRITIQUE", "100"),
                    new Parametre("SEUIL_RETARD_ATTENTION", "15"),
                    new Parametre("SEUIL_RETARD_CRITIQUE", "30"),
                    new Parametre("DELAI_VALIDATION_JOURS", "3"),
                    new Parametre("MAX_HEURES_JOUR_INFO", "10"),
                    new Parametre("MAX_HEURES_SEMAINE_INFO", "48")
            ));
        }

        // 2. Initialiser les types de projet CDG
        if (typeProjetRepository.count() == 0) {
            typeProjetRepository.saveAll(List.of(
                    new TypeProjet("Digital Factory - Web / Mobile"),
                    new TypeProjet("Système d'Information Épargne"),
                    new TypeProjet("Modernisation Prévoyance"),
                    new TypeProjet("Infrastructure Cloud & Data"),
                    new TypeProjet("Cybersécurité & Conformité")
            ));
        }

        // 3. Initialiser les utilisateurs par défaut
        if (utilisateurRepository.count() == 0) {
            String defaultPassword = passwordEncoder.encode("password123");

            Utilisateur admin = utilisateurRepository.save(new Utilisateur(
                    "BENLOUALI", "Ilyas", "admin@cdg.ma", defaultPassword, Role.ADMIN, true
            ));

            Utilisateur cdpAlami = utilisateurRepository.save(new Utilisateur(
                    "EL ALAMI", "Karim", "cdp.alami@cdg.ma", defaultPassword, Role.CHEF_PROJET, true
            ));

            Utilisateur cdpBennani = utilisateurRepository.save(new Utilisateur(
                    "BENNANI", "Sofia", "cdp.bennani@cdg.ma", defaultPassword, Role.CHEF_PROJET, true
            ));

            Utilisateur userMehdi = utilisateurRepository.save(new Utilisateur(
                    "TAZI", "Mehdi", "mehdi.tazi@freelance.cdg.ma", defaultPassword, Role.FREELANCE, true
            ));

            Utilisateur userYoussef = utilisateurRepository.save(new Utilisateur(
                    "CHRAIBI", "Youssef", "youssef.chraibi@freelance.cdg.ma", defaultPassword, Role.FREELANCE, true
            ));

            Utilisateur userIDRISSI = utilisateurRepository.save(new Utilisateur(
                    "IDRISSI", "Fatima", "fatima.idrissi@freelance.cdg.ma", defaultPassword, Role.FREELANCE, true
            ));

            // Profils freelances
            Freelance fMehdi = freelanceRepository.save(new Freelance(
                    userMehdi, new BigDecimal("5000.00"), "Développeur Fullstack Angular / Spring Boot", "ACTIF"
            ));

            Freelance fYoussef = freelanceRepository.save(new Freelance(
                    userYoussef, new BigDecimal("4500.00"), "Ingénieur Data & Python FastAPI", "ACTIF"
            ));

            Freelance fFatima = freelanceRepository.save(new Freelance(
                    userIDRISSI, new BigDecimal("4800.00"), "Expert DevOps & Cloud Docker", "ACTIF"
            ));

        }

        initializeDemoProject();
    }

    private void initializeDemoProject() {
        if (projetRepository.count() > 0) {
            return;
        }

        TypeProjet tpWeb = typeProjetRepository
                .findByLibelle("Digital Factory - Web / Mobile")
                .orElse(null);
        Utilisateur cdpAlami = utilisateurRepository
                .findByEmail("cdp.alami@cdg.ma")
                .orElse(null);
        Freelance fMehdi = freelanceRepository
                .findByUtilisateurEmail("mehdi.tazi@freelance.cdg.ma")
                .orElse(null);

        if (tpWeb == null || cdpAlami == null || fMehdi == null) {
            logger.warn("Skipping demo project initialization because its default project type, project manager, or freelance is missing.");
            return;
        }

        Projet p1 = new Projet();
        p1.setCode("PRJ-CDG-PORTAIL-2026");
        p1.setNom("Portail Numérique Épargne & Prévoyance CDG");
        p1.setDescription("Refonte du portail web adhérents avec espace personnel et déclaration en ligne.");
        p1.setBudget(new BigDecimal("350000.00"));
        p1.setHeuresPrevues(new BigDecimal("400.00"));
        p1.setNombreTachesPrevues(2);
        p1.setDateDebut(LocalDate.now().minusMonths(2));
        p1.setDateFinPrevue(LocalDate.now().plusMonths(4));
        p1.setStatut(ProjetStatut.EN_COURS);
        p1.setTypeProjet(tpWeb);
        p1.setChefProjet(cdpAlami);
        Projet savedP1 = projetRepository.save(p1);

        Affectation affMehdi = new Affectation();
        affMehdi.setProjet(savedP1);
        affMehdi.setFreelance(fMehdi);
        affMehdi.setTjmApplique(new BigDecimal("5000.00"));
        affMehdi.setDateDebut(p1.getDateDebut());
        affMehdi.setDateFin(p1.getDateFinPrevue());
        Affectation savedAff = affectationRepository.save(affMehdi);

        Tache t1 = new Tache();
        t1.setProjet(savedP1);
        t1.setAffectation(savedAff);
        t1.setTitre("Architecture des microservices & API Gateway");
        t1.setChargeEstimee(new BigDecimal("20.0"));
        t1.setAvancement(75);
        t1.setStatut(TacheStatut.EN_COURS);
        t1.setDateDebut(p1.getDateDebut());
        t1.setDateFin(p1.getDateDebut().plusWeeks(6));
        tacheRepository.save(t1);

        Tache t2 = new Tache();
        t2.setProjet(savedP1);
        t2.setAffectation(savedAff);
        t2.setTitre("Développement des composants Frontend Angular & Material");
        t2.setChargeEstimee(new BigDecimal("30.0"));
        t2.setAvancement(40);
        t2.setStatut(TacheStatut.EN_COURS);
        t2.setDateDebut(p1.getDateDebut().plusWeeks(3));
        t2.setDateFin(p1.getDateDebut().plusWeeks(10));
        tacheRepository.save(t2);
    }
}
