package com.cdg.freelance.service;

import com.cdg.freelance.dto.tache.CreateTacheRequest;
import com.cdg.freelance.dto.tache.TacheDto;
import com.cdg.freelance.dto.tache.UpdateAvancementRequest;
import com.cdg.freelance.dto.tache.UpdateTacheRequest;
import com.cdg.freelance.entity.Affectation;
import com.cdg.freelance.entity.Freelance;
import com.cdg.freelance.entity.Projet;
import com.cdg.freelance.entity.Role;
import com.cdg.freelance.entity.Tache;
import com.cdg.freelance.entity.TacheStatut;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AffectationRepository;
import com.cdg.freelance.repository.FreelanceRepository;
import com.cdg.freelance.repository.ProjetRepository;
import com.cdg.freelance.repository.TacheRepository;
import com.cdg.freelance.repository.LigneTimesheetRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

@Service
public class TacheService {

    private final TacheRepository tacheRepository;
    private final ProjetRepository projetRepository;
    private final AffectationRepository affectationRepository;
    private final FreelanceRepository freelanceRepository;
    private final EntityMapper mapper;
    private final LigneTimesheetRepository ligneTimesheetRepository;

    public TacheService(TacheRepository tacheRepository,
                        ProjetRepository projetRepository,
                        AffectationRepository affectationRepository,
                        FreelanceRepository freelanceRepository,
                        EntityMapper mapper,
                        LigneTimesheetRepository ligneTimesheetRepository) {
        this.tacheRepository = tacheRepository;
        this.projetRepository = projetRepository;
        this.affectationRepository = affectationRepository;
        this.freelanceRepository = freelanceRepository;
        this.mapper = mapper;
        this.ligneTimesheetRepository = ligneTimesheetRepository;
    }

    @Transactional(readOnly = true)
    public List<TacheDto> listMesMissions(UserPrincipal currentUser) {
        return listMesMissions(currentUser, false);
    }

    @Transactional(readOnly = true)
    public List<TacheDto> listMesMissions(UserPrincipal currentUser, boolean disponibles) {
        if (currentUser.getRole() != Role.FREELANCE) {
            throw new AccessDeniedException("Cette liste est réservée au freelance connecté");
        }
        Freelance freelance = freelanceRepository.findByUtilisateurId(currentUser.getId())
                .orElseThrow(() -> new BusinessException("Aucun profil freelance n'est associé à votre compte"));
        List<Tache> tasks = tacheRepository.findAllByAffectationFreelanceId(freelance.getId());
        if (tasks.isEmpty()) return List.of();
        Map<Long, LigneTimesheetRepository.HeuresParTache> totals = ligneTimesheetRepository
                .sumHeuresByTacheIds(tasks.stream().map(Tache::getId).toList()).stream()
                .collect(Collectors.toMap(LigneTimesheetRepository.HeuresParTache::getTacheId, Function.identity()));
        return tasks.stream()
                .map(task -> {
                    TacheDto dto = mapper.toTacheDto(task);
                    LigneTimesheetRepository.HeuresParTache total = totals.get(task.getId());
                    dto.setHeuresSaisies(total == null ? BigDecimal.ZERO : total.getHeures());
                    return dto;
                })
                .filter(dto -> !disponibles || dto.getHeuresSaisies().compareTo(dto.getHeuresPrevues()) < 0)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheDto> listTachesByProjet(Long projetId) {
        return tacheRepository.findAllByProjetId(projetId).stream()
                .map(mapper::toTacheDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TacheDto getTacheById(Long id) {
        Tache t = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));
        return mapper.toTacheDto(t);
    }

    @Transactional
    public TacheDto createTache(Long projetId, CreateTacheRequest req) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'id : " + projetId));
        validateTaskDates(projet, req.getDateDebut(), req.getDateFin());

        Affectation affectation = null;
        if (req.getAffectationId() != null) {
            affectation = affectationRepository.findById(req.getAffectationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Affectation non trouvée"));
            if (!affectation.getProjet().getId().equals(projetId)) {
                throw new BusinessException("L'affectation choisie n'appartient pas à ce projet");
            }
            if (req.getDateDebut() != null
                    && !isAffectionActiveForDate(affectation, req.getDateDebut())) {
                throw new BusinessException("L'affectation choisie n'est pas active à la date de début de la tâche");
            }
            if (req.getDateFin() != null
                    && !isAffectionActiveForDate(affectation, req.getDateFin())) {
                throw new BusinessException("L'affectation choisie n'est pas active à la date de fin de la tâche");
            }
        }
        if (affectation != null) {
            validateSimultaneousTaskLimit(affectation, null, req.getDateDebut(), req.getDateFin());
        }

        Tache t = new Tache();
        t.setProjet(projet);
        t.setAffectation(affectation);
        t.setTitre(req.getTitre().trim());
        BigDecimal hours = req.getHeuresPrevues();
        if (hours == null && req.getChargeEstimee() != null) {
            hours = req.getChargeEstimee().multiply(BigDecimal.valueOf(8));
        }
        if (hours == null) {
            if (projet.getHeuresPrevues() == null || projet.getNombreTachesPrevues() == null
                    || projet.getNombreTachesPrevues() < 1) {
                throw new BusinessException("Renseignez les heures et le nombre de tâches prévus du projet ou les heures de la tâche");
            }
            hours = projet.getHeuresPrevues().divide(BigDecimal.valueOf(projet.getNombreTachesPrevues()), 2, RoundingMode.HALF_UP);
        }
        if (hours.signum() <= 0) {
            throw new BusinessException("Les heures prévues de la tâche doivent être supérieures à zéro");
        }
        setTaskHours(t, hours);
        t.setAvancement(0);
        t.setStatut(TacheStatut.A_FAIRE);
        t.setDateDebut(req.getDateDebut());
        t.setDateFin(req.getDateFin());

        Tache saved = tacheRepository.save(t);
        return mapper.toTacheDto(saved);
    }

    @Transactional
    public TacheDto updateTache(Long id, UpdateTacheRequest req) {
        Tache t = tacheRepository.findForUpdateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));
        validateTaskDates(t.getProjet(), req.getDateDebut(), req.getDateFin());

        t.setTitre(req.getTitre().trim());
        BigDecimal hours = req.getHeuresPrevues();
        if (hours == null && req.getChargeEstimee() != null) {
            hours = req.getChargeEstimee().multiply(BigDecimal.valueOf(8));
        }
        if (hours == null || hours.signum() <= 0) {
            throw new BusinessException("Les heures prévues de la tâche doivent être supérieures à zéro");
        }
        if (hours.compareTo(t.getHeuresPrevues()) < 0
                && hours.compareTo(ligneTimesheetRepository.sumHeuresByTache(id, null)) < 0) {
            throw new BusinessException("Les heures prévues ne peuvent pas être inférieures aux heures déjà saisies");
        }
        setTaskHours(t, hours);
        if (req.getAvancement() != null) {
            t.setAvancement(req.getAvancement());
            if (req.getAvancement() == 100) {
                t.setStatut(TacheStatut.TERMINEE);
            } else if (req.getAvancement() > 0 && t.getStatut() == TacheStatut.A_FAIRE) {
                t.setStatut(TacheStatut.EN_COURS);
            } else if (req.getAvancement() == 0) {
                t.setStatut(TacheStatut.A_FAIRE);
            }
        }
        if (req.getStatut() != null) {
            t.setStatut(req.getStatut());
        }
        if (req.getAffectationId() != null) {
            Affectation aff = affectationRepository.findById(req.getAffectationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Affectation non trouvée"));
            if (!aff.getProjet().getId().equals(t.getProjet().getId())) {
                throw new BusinessException("L'affectation n'appartient pas au projet de la tâche");
            }
            t.setAffectation(aff);
        }
        if (t.getAffectation() != null) {
            validateSimultaneousTaskLimit(t.getAffectation(), t.getId(), req.getDateDebut(), req.getDateFin());
        }
        t.setDateDebut(req.getDateDebut());
        t.setDateFin(req.getDateFin());

        Tache updated = tacheRepository.save(t);
        return mapper.toTacheDto(updated);
    }

    @Transactional
    public TacheDto updateAvancement(Long id, UpdateAvancementRequest req, UserPrincipal currentUser) {
        Tache t = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));

        if (currentUser.getRole() != Role.CHEF_PROJET
                || t.getProjet() == null
                || t.getProjet().getChefProjet() == null
                || !t.getProjet().getChefProjet().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Seul le chef de projet concerné peut modifier l'avancement de cette tâche");
        }

        t.setAvancement(req.getAvancement());
        if (req.getAvancement() == 100) {
            t.setStatut(TacheStatut.TERMINEE);
        } else if (req.getAvancement() > 0 && t.getStatut() == TacheStatut.A_FAIRE) {
            t.setStatut(TacheStatut.EN_COURS);
        } else if (req.getAvancement() == 0) {
            t.setStatut(TacheStatut.A_FAIRE);
        }

        Tache updated = tacheRepository.save(t);
        return mapper.toTacheDto(updated);
    }

    private void setTaskHours(Tache tache, BigDecimal hours) {
        if (hours.compareTo(new BigDecimal("799999.20")) > 0) {
            throw new BusinessException("Les heures prévues dépassent la limite autorisée");
        }
        tache.setHeuresPrevues(hours);
        tache.setChargeEstimee(hours.divide(BigDecimal.valueOf(8), 1, RoundingMode.HALF_UP));
    }

    private void validateTaskDates(Projet projet, java.time.LocalDate dateDebut, java.time.LocalDate dateFin) {
        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new BusinessException("La date de fin de la tâche doit être postérieure ou égale à sa date de début");
        }
        if (projet != null && dateDebut != null && projet.getDateDebut() != null
                && dateDebut.isBefore(projet.getDateDebut())) {
            throw new BusinessException("La date de début de la tâche ne peut pas précéder celle du projet");
        }
        if (projet != null && dateFin != null && projet.getDateFinPrevue() != null
                && dateFin.isAfter(projet.getDateFinPrevue())) {
            throw new BusinessException("La date de fin de la tâche ne peut pas dépasser celle du projet");
        }
    }

    private boolean isAffectionActiveForDate(Affectation affectation, java.time.LocalDate date) {
        if (affectation == null || affectation.getDateDebut() == null
                || date.isBefore(affectation.getDateDebut())) {
            return false;
        }
        return affectation.getDateFin() == null || !date.isAfter(affectation.getDateFin());
    }

    private void validateSimultaneousTaskLimit(Affectation affectation, Long excludedTaskId,
                                               java.time.LocalDate start, java.time.LocalDate end) {
        if (affectation == null || affectation.getFreelance() == null) {
            return;
        }

        java.time.LocalDate periodStart = start != null ? start : affectation.getDateDebut();
        java.time.LocalDate periodEnd = end != null ? end : affectation.getDateFin();
        int overlappingActiveTasks = 0;

        for (Tache existing : tacheRepository.findAllByAffectationFreelanceId(affectation.getFreelance().getId())) {
            if (excludedTaskId != null && excludedTaskId.equals(existing.getId())) {
                continue;
            }
            if (existing.getStatut() == TacheStatut.TERMINEE) {
                continue;
            }
            if (periodsOverlap(periodStart, periodEnd, existing.getDateDebut(), existing.getDateFin())) {
                overlappingActiveTasks++;
            }
        }

        if (overlappingActiveTasks >= 4) {
            throw new BusinessException(
                    "Le freelance ne peut pas avoir plus de quatre tâches non terminées simultanément.");
        }
    }

    private boolean periodsOverlap(java.time.LocalDate startA, java.time.LocalDate endA,
                                   java.time.LocalDate startB, java.time.LocalDate endB) {
        if (startA != null && endB != null && endB.isBefore(startA)) {
            return false;
        }
        if (startB != null && endA != null && endA.isBefore(startB)) {
            return false;
        }
        return true;
    }
}
