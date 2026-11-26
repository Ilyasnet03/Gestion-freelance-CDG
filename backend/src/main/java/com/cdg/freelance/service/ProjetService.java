package com.cdg.freelance.service;

import com.cdg.freelance.dto.projet.*;
import com.cdg.freelance.dto.admin.TypeProjetDto;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.*;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final TypeProjetRepository typeProjetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FreelanceRepository freelanceRepository;
    private final AffectationRepository affectationRepository;
    private final TacheRepository tacheRepository;
    private final LigneTimesheetRepository ligneTimesheetRepository;
    private final PvReceptionRepository pvReceptionRepository;
    private final EntityMapper mapper;

    public ProjetService(ProjetRepository projetRepository,
                         TypeProjetRepository typeProjetRepository,
                         UtilisateurRepository utilisateurRepository,
                         FreelanceRepository freelanceRepository,
                         AffectationRepository affectationRepository,
                         TacheRepository tacheRepository,
                         LigneTimesheetRepository ligneTimesheetRepository,
                         PvReceptionRepository pvReceptionRepository,
                         EntityMapper mapper) {
        this.projetRepository = projetRepository;
        this.typeProjetRepository = typeProjetRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.freelanceRepository = freelanceRepository;
        this.affectationRepository = affectationRepository;
        this.tacheRepository = tacheRepository;
        this.ligneTimesheetRepository = ligneTimesheetRepository;
        this.pvReceptionRepository = pvReceptionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProjetDto> listProjets(UserPrincipal currentUser) {
        List<Projet> projets;
        if (currentUser.getRole() == Role.ADMIN) {
            projets = projetRepository.findAll();
        } else if (currentUser.getRole() == Role.CHEF_PROJET) {
            projets = projetRepository.findAllByChefProjetId(currentUser.getId());
        } else {
            throw new AccessDeniedException("Seuls les administrateurs et chefs de projet peuvent consulter les projets");
        }

        Map<Long, BigDecimal> budgets = calculateBudgetsConsommes();
        return projets.stream().map(p -> {
            ProjetDto dto = mapper.toProjetDto(p);
            enrichProjetDtoWithAggregates(dto, p.getId(), p.getBudget(), budgets);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TypeProjetDto> listTypesProjet() {
        return typeProjetRepository.findAll().stream()
                .map(mapper::toTypeProjetDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjetDto getProjetById(Long id, UserPrincipal currentUser) {
        Projet p = projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'id : " + id));
        ensureProjectAccess(p, currentUser);
        ProjetDto dto = mapper.toProjetDto(p);
        enrichProjetDtoWithAggregates(dto, p.getId(), p.getBudget());
        return dto;
    }

    @Transactional
    public ProjetDto createProjet(CreateProjetRequest req, UserPrincipal currentUser) {
        requireProjectManager(currentUser);
        if (projetRepository.existsByCode(req.getCode().trim().toUpperCase())) {
            throw new BusinessException("Un projet avec ce code existe déjà : " + req.getCode());
        }

        if (req.getDateFinPrevue().isBefore(req.getDateDebut())) {
            throw new BusinessException("La date de fin prévue doit être postérieure à la date de début");
        }

        TypeProjet tp = typeProjetRepository.findById(req.getTypeProjetId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de projet non trouvé : " + req.getTypeProjetId()));

        Utilisateur cdp = utilisateurRepository.findById(req.getChefProjetId())
                .orElseThrow(() -> new ResourceNotFoundException("Chef de projet non trouvé : " + req.getChefProjetId()));

        if (cdp.getRole() != Role.CHEF_PROJET && cdp.getRole() != Role.ADMIN) {
            throw new BusinessException("L'utilisateur sélectionné n'a pas le rôle CHEF_PROJET");
        }
        if (currentUser.getRole() == Role.CHEF_PROJET && !cdp.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Un chef de projet ne peut créer un projet que pour son propre compte");
        }

        Projet p = new Projet();
        p.setCode(req.getCode().trim().toUpperCase());
        p.setNom(req.getNom().trim());
        p.setDescription(req.getDescription());
        p.setBudget(req.getBudget());
        p.setHeuresPrevues(req.getHeuresPrevues());
        p.setNombreTachesPrevues(req.getNombreTachesPrevues());
        p.setDateDebut(req.getDateDebut());
        p.setDateFinPrevue(req.getDateFinPrevue());
        p.setStatut(ProjetStatut.PLANIFIE);
        p.setTypeProjet(tp);
        p.setChefProjet(cdp);

        Projet saved = projetRepository.save(p);
        return mapper.toProjetDto(saved);
    }

    @Transactional
    public ProjetDto updateProjet(Long id, UpdateProjetRequest req, UserPrincipal currentUser) {
        requireProjectManager(currentUser);
        Projet p = projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'id : " + id));
        ensureProjectAccess(p, currentUser);

        if (currentUser.getRole() == Role.CHEF_PROJET
                && req.getChefProjetId() != null
                && !req.getChefProjetId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Un chef de projet ne peut pas transférer son projet à un autre compte");
        }

        if (req.getDateFinPrevue().isBefore(req.getDateDebut())) {
            throw new BusinessException("La date de fin prévue doit être postérieure à la date de début");
        }

        p.setNom(req.getNom().trim());
        p.setDescription(req.getDescription());
        p.setBudget(req.getBudget());
        if (req.getHeuresPrevues() != null) p.setHeuresPrevues(req.getHeuresPrevues());
        if (req.getNombreTachesPrevues() != null) p.setNombreTachesPrevues(req.getNombreTachesPrevues());
        p.setDateDebut(req.getDateDebut());
        p.setDateFinPrevue(req.getDateFinPrevue());

        if (req.getStatut() != null) {
            p.setStatut(req.getStatut());
        }

        if (req.getTypeProjetId() != null) {
            TypeProjet tp = typeProjetRepository.findById(req.getTypeProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Type de projet introuvable"));
            p.setTypeProjet(tp);
        }

        if (req.getChefProjetId() != null) {
            Utilisateur cdp = utilisateurRepository.findById(req.getChefProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chef de projet introuvable"));
            p.setChefProjet(cdp);
        }

        Projet updated = projetRepository.save(p);
        return mapper.toProjetDto(updated);
    }

    @Transactional
    public AffectationDto addAffectation(Long projetId, CreateAffectationRequest req, UserPrincipal currentUser) {
        requireProjectManager(currentUser);
        if (req.getDateFin() != null && req.getDateFin().isBefore(req.getDateDebut())) {
            throw new BusinessException("La date de fin de l'affectation doit être postérieure ou égale à sa date de début");
        }
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'id : " + projetId));
        ensureProjectAccess(projet, currentUser);

        Freelance freelance = freelanceRepository.findById(req.getFreelanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Freelance non trouvé avec l'id : " + req.getFreelanceId()));

        // RG2 : un freelance a au plus une affectation active à la fois
        List<Affectation> activeAffectations = affectationRepository.findActiveAffectationsByFreelanceAndDate(
                freelance.getId(), req.getDateDebut());

        for (Affectation existing : activeAffectations) {
            // Check date overlap
            boolean overlap = true;
            if (req.getDateFin() != null && existing.getDateDebut().isAfter(req.getDateFin())) {
                overlap = false;
            }
            if (existing.getDateFin() != null && req.getDateDebut().isAfter(existing.getDateFin())) {
                overlap = false;
            }
            if (overlap) {
                throw new BusinessException("RG2 : Le freelance " + freelance.getUtilisateur().getPrenom() + " " +
                        freelance.getUtilisateur().getNom() + " a déjà une affectation active sur le projet " +
                        existing.getProjet().getNom() + " sur cette période.");
            }
        }

        Affectation affectation = new Affectation();
        affectation.setProjet(projet);
        affectation.setFreelance(freelance);
        affectation.setTjmApplique(req.getTjmApplique());
        affectation.setDateDebut(req.getDateDebut());
        affectation.setDateFin(req.getDateFin());

        Affectation saved = affectationRepository.save(affectation);
        return mapper.toAffectationDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AffectationDto> listAffectationsByProjet(Long projetId, UserPrincipal currentUser) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'id : " + projetId));
        ensureProjectAccess(projet, currentUser);
        return affectationRepository.findAllByProjetId(projetId).stream()
                .map(mapper::toAffectationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjetAvancementDto getAvancementAndBudget(Long projetId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'id : " + projetId));

        ProjetAvancementDto dto = new ProjetAvancementDto();
        dto.setProjetId(projet.getId());
        dto.setCode(projet.getCode());
        dto.setNom(projet.getNom());
        dto.setBudget(projet.getBudget());
        dto.setDateDebut(projet.getDateDebut());
        dto.setDateFinPrevue(projet.getDateFinPrevue());

        // Calcul du budget consommé via les lignes de timesheets validées
        BigDecimal budgetConsomme = calculateBudgetsConsommes()
                .getOrDefault(projetId, BigDecimal.ZERO);
        dto.setBudgetConsomme(budgetConsomme);

        // Taux de consommation : (budgetConsommé / budget) * 100
        double taux = 0.0;
        if (projet.getBudget().compareTo(BigDecimal.ZERO) > 0) {
            taux = budgetConsomme.divide(projet.getBudget(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        dto.setTauxConsommation(Math.round(taux * 100.0) / 100.0);

        // Avancement réel : moyenne pondérée ou simple des tâches du projet
        List<Tache> taches = tacheRepository.findAllByProjetId(projetId);
        int avancementReel = 0;
        if (!taches.isEmpty()) {
            double totalWeight = taches.stream()
                    .mapToDouble(this::taskWeight)
                    .sum();
            double progressSum = taches.stream()
                    .mapToDouble(t -> {
                        double weight = taskWeight(t);
                        return (t.getAvancement() != null ? t.getAvancement() : 0) * weight;
                    }).sum();
            avancementReel = (int) Math.round(progressSum / totalWeight);
        }
        dto.setAvancementReel(avancementReel);

        // Jours écoulés et durée prévue
        LocalDate today = LocalDate.now();
        long dureePrevue = Math.max(1, ChronoUnit.DAYS.between(projet.getDateDebut(), projet.getDateFinPrevue()));
        long joursEcoules = Math.max(0, ChronoUnit.DAYS.between(projet.getDateDebut(), today));
        if (today.isAfter(projet.getDateFinPrevue())) {
            joursEcoules = dureePrevue;
        }
        dto.setDureePrevueJours(dureePrevue);
        dto.setJoursEcoules(joursEcoules);

        // Avancement attendu : (jours écoulés / durée prévue) * 100
        double avancementAttendu = Math.min(100.0, ((double) joursEcoules / (double) dureePrevue) * 100.0);
        dto.setAvancementAttendu(Math.round(avancementAttendu * 100.0) / 100.0);

        // Écart d'avancement (formule section 10.3) : avancementAttendu - avancementReel
        double ecart = avancementAttendu - (double) avancementReel;
        dto.setEcartAvancement(Math.round(ecart * 100.0) / 100.0);

        return dto;
    }

    @Transactional(readOnly = true)
    public ProjetAvancementDto getAvancementAndBudget(Long projetId, UserPrincipal currentUser) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'id : " + projetId));
        ensureProjectAccess(projet, currentUser);
        return getAvancementAndBudget(projetId);
    }

    @Transactional(readOnly = true)
    public void ensureProjectAccess(Long projetId, UserPrincipal currentUser) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'id : " + projetId));
        ensureProjectAccess(projet, currentUser);
    }

    private void ensureProjectAccess(Projet projet, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() == Role.CHEF_PROJET
                && projet.getChefProjet() != null
                && projet.getChefProjet().getId().equals(currentUser.getId())) {
            return;
        }
        throw new AccessDeniedException("Ce projet ne relève pas de votre périmètre");
    }

    private void requireProjectManager(UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.CHEF_PROJET) {
            throw new AccessDeniedException("Seul un chef de projet peut modifier les projets");
        }
    }

    public BigDecimal calculateBudgetConsomme(Long projetId) {
        Map<Long, BigDecimal> budgets = calculateBudgetsConsommes();
        return budgets.getOrDefault(projetId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public Map<Long, BigDecimal> calculateBudgetsConsommes() {
        Map<Long, BigDecimal> budgets = new HashMap<>();
        Set<Long> invoicedTimesheetIds = new HashSet<>();
        Map<Long, List<LigneTimesheet>> linesByTimesheet = new HashMap<>();
        List<LigneTimesheet> allLines = ligneTimesheetRepository.findAll();
        for (LigneTimesheet line : allLines) {
            if (line.getTimesheet() != null) {
                linesByTimesheet.computeIfAbsent(line.getTimesheet().getId(), key -> new java.util.ArrayList<>())
                        .add(line);
            }
        }

        for (PvReception pv : pvReceptionRepository.findAll()) {
            Timesheet timesheet = pv.getTimesheet();
            if (timesheet == null || timesheet.getStatut() != TimesheetStatut.VALIDEE
                    || pv.getMontant() == null) {
                continue;
            }

            List<LigneTimesheet> lignesTimesheet =
                    linesByTimesheet.getOrDefault(timesheet.getId(), List.of());
            if (lignesTimesheet.isEmpty()) {
                continue;
            }

            BigDecimal totalHeures = lignesTimesheet.stream()
                    .map(LigneTimesheet::getHeures)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<Long, BigDecimal> heuresByProjet = new HashMap<>();
            for (LigneTimesheet ligne : lignesTimesheet) {
                if (ligne.getTache() != null && ligne.getTache().getProjet() != null
                        && ligne.getHeures() != null) {
                    heuresByProjet.merge(ligne.getTache().getProjet().getId(),
                            ligne.getHeures(), BigDecimal::add);
                }
            }

            if (totalHeures.compareTo(BigDecimal.ZERO) > 0) {
                invoicedTimesheetIds.add(timesheet.getId());
                for (Map.Entry<Long, BigDecimal> entry : heuresByProjet.entrySet()) {
                    BigDecimal partProjet = entry.getValue().divide(totalHeures, 6, RoundingMode.HALF_UP);
                    BigDecimal montantProjet = pv.getMontant()
                            .multiply(partProjet)
                            .setScale(2, RoundingMode.HALF_UP);
                    budgets.merge(entry.getKey(), montantProjet, BigDecimal::add);
                }
            }
        }

        List<Tache> taches = tacheRepository.findAll();
        for (Tache t : taches) {
            BigDecimal tjm = BigDecimal.valueOf(4500);
            if (t.getAffectation() != null && t.getAffectation().getTjmApplique() != null) {
                tjm = t.getAffectation().getTjmApplique();
            }
            List<LigneTimesheet> lignes = allLines.stream()
                    .filter(l -> l.getTache() != null && Objects.equals(l.getTache().getId(), t.getId()))
                    .filter(l -> l.getTimesheet() != null
                            && l.getTimesheet().getStatut() == TimesheetStatut.VALIDEE)
                    .filter(l -> !invoicedTimesheetIds.contains(l.getTimesheet().getId()))
                    .collect(Collectors.toList());

            for (LigneTimesheet l : lignes) {
                BigDecimal jours = l.getHeures().divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);
                if (t.getProjet() != null) {
                    budgets.merge(t.getProjet().getId(),
                            jours.multiply(tjm).setScale(2, RoundingMode.HALF_UP),
                            BigDecimal::add);
                }
            }
        }

        return budgets;
    }

    private void enrichProjetDtoWithAggregates(ProjetDto dto, Long projetId, BigDecimal budget) {
        enrichProjetDtoWithAggregates(dto, projetId, budget, calculateBudgetsConsommes());
    }

    private void enrichProjetDtoWithAggregates(ProjetDto dto, Long projetId, BigDecimal budget,
                                               Map<Long, BigDecimal> budgets) {
        BigDecimal consomme = budgets.getOrDefault(projetId, BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        dto.setBudgetConsomme(consomme);
        double taux = 0.0;
        if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
            taux = consomme.divide(budget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        dto.setTauxConsommation(Math.round(taux * 100.0) / 100.0);

        List<Tache> taches = tacheRepository.findAllByProjetId(projetId);
        if (!taches.isEmpty()) {
            double totalWeight = taches.stream()
                    .mapToDouble(this::taskWeight)
                    .sum();
            double progressSum = taches.stream()
                    .mapToDouble(t -> {
                        double weight = taskWeight(t);
                        return (t.getAvancement() != null ? t.getAvancement() : 0) * weight;
                    })
                    .sum();
            dto.setAvancementGlobal((int) Math.round(progressSum / totalWeight));
        } else {
            dto.setAvancementGlobal(0);
        }

    }

    private double taskWeight(Tache task) {
        return task.getHeuresPrevues().signum() > 0 ? task.getHeuresPrevues().doubleValue() : 8.0;
    }
}
