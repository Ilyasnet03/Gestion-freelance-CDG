package com.cdg.freelance.service;

import com.cdg.freelance.dto.timesheet.*;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.exception.ResourceNotFoundException;
import com.cdg.freelance.exception.UnauthorizedException;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.*;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final LigneTimesheetRepository ligneTimesheetRepository;
    private final FreelanceRepository freelanceRepository;
    private final TacheRepository tacheRepository;
    private final AffectationRepository affectationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PvReceptionRepository pvReceptionRepository;
    private final PvService pvService;
    private final EntityMapper mapper;

    public TimesheetService(TimesheetRepository timesheetRepository,
                            LigneTimesheetRepository ligneTimesheetRepository,
                            FreelanceRepository freelanceRepository,
                            TacheRepository tacheRepository,
                            AffectationRepository affectationRepository,
                            UtilisateurRepository utilisateurRepository,
                            PvReceptionRepository pvReceptionRepository,
                            PvService pvService,
                            EntityMapper mapper) {
        this.timesheetRepository = timesheetRepository;
        this.ligneTimesheetRepository = ligneTimesheetRepository;
        this.freelanceRepository = freelanceRepository;
        this.tacheRepository = tacheRepository;
        this.affectationRepository = affectationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.pvReceptionRepository = pvReceptionRepository;
        this.pvService = pvService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TimesheetDto> listTimesheets(LocalDate semaine, UserPrincipal currentUser) {
        return listTimesheets(semaine, null, currentUser);
    }

    @Transactional(readOnly = true)
    public List<TimesheetDto> listTimesheets(LocalDate semaine, TimesheetStatut statut, UserPrincipal currentUser) {
        List<Timesheet> timesheets;

        if (currentUser.getRole() == Role.FREELANCE) {
            Freelance freelance = getFreelanceForUser(currentUser.getId());
            if (semaine != null) {
                timesheets = timesheetRepository.findByFreelanceIdAndSemaineDebut(freelance.getId(), semaine)
                        .map(List::of)
                        .orElse(List.of());
            } else {
                timesheets = timesheetRepository.findAllByFreelanceId(freelance.getId());
            }
        } else if (currentUser.getRole() == Role.CHEF_PROJET) {
            List<Timesheet> candidates;
            if (semaine != null) {
                candidates = timesheetRepository.findAllBySemaineDebut(semaine);
            } else {
                candidates = timesheetRepository.findAll();
            }
            timesheets = candidates.stream()
                    .filter(t -> statut == null || t.getStatut() == statut)
                    .collect(Collectors.toList());

            if (!timesheets.isEmpty()) {
                List<Long> ids = timesheets.stream().map(Timesheet::getId).collect(Collectors.toList());
                Map<Long, List<LigneTimesheet>> linesByTimesheet = new HashMap<>();
                for (LigneTimesheet line : ligneTimesheetRepository.findAllByTimesheetIdIn(ids)) {
                    linesByTimesheet.computeIfAbsent(line.getTimesheet().getId(), key -> new java.util.ArrayList<>())
                            .add(line);
                }
                timesheets = timesheets.stream()
                        .filter(t -> isTimesheetManagedByChef(t, currentUser.getId(),
                                linesByTimesheet.getOrDefault(t.getId(), List.of())))
                        .collect(Collectors.toList());
            }
        } else {
            // ADMIN
            if (semaine != null) {
                timesheets = timesheetRepository.findAllBySemaineDebut(semaine);
            } else {
                timesheets = timesheetRepository.findAll();
            }
        }

        timesheets = timesheets.stream()
                .filter(t -> statut == null || t.getStatut() == statut)
                .collect(Collectors.toList());
        if (timesheets.isEmpty()) {
            return List.of();
        }

        List<Long> timesheetIds = timesheets.stream()
                .map(Timesheet::getId)
                .collect(Collectors.toList());
        Map<Long, List<LigneTimesheet>> lignesByTimesheet = new HashMap<>();
        for (LigneTimesheet ligne : ligneTimesheetRepository.findAllByTimesheetIdIn(timesheetIds)) {
            lignesByTimesheet.computeIfAbsent(ligne.getTimesheet().getId(), key -> new java.util.ArrayList<>())
                    .add(ligne);
        }
        Map<Long, PvReception> pvByTimesheet = new HashMap<>();
        for (PvReception pv : pvReceptionRepository.findAllByTimesheetIdIn(timesheetIds)) {
            pvByTimesheet.put(pv.getTimesheet().getId(), pv);
        }

        return timesheets.stream().map(t -> {
            TimesheetDto dto = mapper.toTimesheetDto(t);
            List<LigneTimesheet> lignes = lignesByTimesheet.getOrDefault(t.getId(), List.of());
            dto.setLignes(lignes.stream().map(mapper::toLigneTimesheetDto).collect(Collectors.toList()));

            BigDecimal totalHeures = lignes.stream()
                    .map(LigneTimesheet::getHeures)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalHeuresSemaine(totalHeures);

            BigDecimal tjm = determineTjmApplique(t, lignes);
            BigDecimal jours = totalHeures.divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);
            dto.setMontantEstime(jours.multiply(tjm).setScale(2, RoundingMode.HALF_UP));

            PvReception pv = pvByTimesheet.get(t.getId());
            if (pv != null) {
                dto.setPvId(pv.getId());
                dto.setNumeroPv(pv.getNumero());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TimesheetDto getTimesheetById(Long id, UserPrincipal currentUser) {
        Timesheet t = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet non trouvée avec l'id : " + id));

        // Vérification d'accès
        if (currentUser.getRole() == Role.FREELANCE) {
            Freelance freelance = getFreelanceForUser(currentUser.getId());
            if (!t.getFreelance().getId().equals(freelance.getId())) {
                throw new UnauthorizedException("Accès non autorisé à cette timesheet");
            }
        } else if (currentUser.getRole() == Role.CHEF_PROJET) {
            if (!isTimesheetManagedByChef(t, currentUser.getId())) {
                throw new UnauthorizedException("Cette timesheet ne relève pas de vos projets");
            }
        }

        return toDetailedDto(t);
    }

    @Transactional
    public TimesheetDto openOrCreateTimesheet(LocalDate semaineDebut, UserPrincipal currentUser) {
        Freelance freelance = getFreelanceForUser(currentUser.getId());

        // RG1 : une timesheet couvre une semaine du lundi au dimanche ; semaine_debut est toujours un lundi
        if (semaineDebut.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessException("RG1 : La date de début de semaine doit impérativement être un lundi");
        }

        return timesheetRepository.findByFreelanceIdAndSemaineDebut(freelance.getId(), semaineDebut)
                .map(this::toDetailedDto)
                .orElseGet(() -> {
                    Timesheet newTs = new Timesheet();
                    newTs.setFreelance(freelance);
                    newTs.setSemaineDebut(semaineDebut);
                    newTs.setStatut(TimesheetStatut.BROUILLON);
                    Timesheet saved = timesheetRepository.save(newTs);
                    return toDetailedDto(saved);
                });
    }

    @Transactional
    public LigneTimesheetDto addOrUpdateLigne(Long timesheetId, AddLigneRequest req, UserPrincipal currentUser) {
        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet non trouvée avec l'id : " + timesheetId));

        Freelance freelance = freelanceRepository.findByUtilisateurIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fiche freelance non trouvée pour cet utilisateur"));

        // Contrôle de propriété
        if (!timesheet.getFreelance().getId().equals(freelance.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas le propriétaire de cette timesheet");
        }

        // RG5 & RG6 : Les lignes ne sont modifiables que si la timesheet est BROUILLON ou REJETEE
        if (timesheet.getStatut() != TimesheetStatut.BROUILLON && timesheet.getStatut() != TimesheetStatut.REJETEE) {
            throw new BusinessException("RG6 : Une timesheet " + timesheet.getStatut() + " est verrouillée et ne peut plus être modifiée");
        }

        // RG1 : La date de saisie doit être comprise entre le lundi et le dimanche de la semaine
        LocalDate lundi = timesheet.getSemaineDebut();
        LocalDate dimanche = lundi.plusDays(6);
        if (req.getDateSaisie().isBefore(lundi) || req.getDateSaisie().isAfter(dimanche)) {
            throw new BusinessException("RG1 : La date de saisie (" + req.getDateSaisie() +
                    ") doit être comprise dans la semaine du " + lundi + " au " + dimanche);
        }

        // Vérification de la tâche
        Tache tache = tacheRepository.findForUpdateById(req.getTacheId())
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable avec l'id : " + req.getTacheId()));

        // RG3 : Le freelance ne saisit que sur les tâches de son projet et à des dates comprises dans la période de son affectation
        List<Affectation> affectations = affectationRepository.findActiveAffectationsByFreelanceAndDate(freelance.getId(), req.getDateSaisie());
        boolean hasValidAffectation = affectations.stream()
                .anyMatch(a -> a.getProjet().getId().equals(tache.getProjet().getId()));

        if (!hasValidAffectation) {
            throw new BusinessException("RG3 : Vous n'avez pas d'affectation active sur le projet '" +
                    tache.getProjet().getNom() + "' à la date du " + req.getDateSaisie());
        }

        // Check if an existing line exists for this day and task
        LigneTimesheet existingLigne = ligneTimesheetRepository
                .findByTimesheetIdAndTacheIdAndDateSaisie(timesheetId, req.getTacheId(), req.getDateSaisie())
                .orElse(null);

        Long excludeId = existingLigne != null ? existingLigne.getId() : null;

        // Le verrou du freelance sérialise les saisies concurrentes de la journée.
        BigDecimal heuresDejaSaisies = ligneTimesheetRepository.sumHeuresByFreelanceAndDate(freelance.getId(), req.getDateSaisie(), excludeId);
        BigDecimal nouveauTotal = heuresDejaSaisies.add(req.getHeures());

        if (nouveauTotal.compareTo(BigDecimal.TEN) > 0) {
            throw new BusinessException("Le total d'heures de la journée dépasse 10 h");
        }

        BigDecimal totalTache = ligneTimesheetRepository.sumHeuresByTache(tache.getId(), excludeId).add(req.getHeures());
        if (totalTache.compareTo(tache.getHeuresPrevues()) > 0) {
            throw new BusinessException("La saisie dépasse les heures prévues pour cette tâche (" + tache.getHeuresPrevues() + " h)");
        }

        LigneTimesheet ligneToSave;
        if (existingLigne != null) {
            existingLigne.setHeures(req.getHeures());
            ligneToSave = existingLigne;
        } else {
            ligneToSave = new LigneTimesheet(timesheet, tache, req.getDateSaisie(), req.getHeures());
        }

        LigneTimesheet saved = ligneTimesheetRepository.save(ligneToSave);
        LigneTimesheetDto dto = mapper.toLigneTimesheetDto(saved);
        if (nouveauTotal.compareTo(freelance.getSeuilHeuresJour()) > 0) {
            dto.setAvertissement("Le total de " + nouveauTotal.stripTrailingZeros().toPlainString()
                    + " h dépasse votre seuil journalier de "
                    + freelance.getSeuilHeuresJour().stripTrailingZeros().toPlainString() + " h.");
        }
        return dto;
    }

    @Transactional
    public void deleteLigne(Long timesheetId, Long ligneId, UserPrincipal currentUser) {
        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet non trouvée avec l'id : " + timesheetId));

        Freelance freelance = getFreelanceForUser(currentUser.getId());
        if (!timesheet.getFreelance().getId().equals(freelance.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas le propriétaire de cette timesheet");
        }

        if (timesheet.getStatut() != TimesheetStatut.BROUILLON && timesheet.getStatut() != TimesheetStatut.REJETEE) {
            throw new BusinessException("RG6 : Modification impossible, la timesheet est " + timesheet.getStatut());
        }

        LigneTimesheet ligne = ligneTimesheetRepository.findById(ligneId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne non trouvée avec l'id : " + ligneId));

        if (!ligne.getTimesheet().getId().equals(timesheetId)) {
            throw new BusinessException("La ligne n'appartient pas à cette timesheet");
        }

        ligneTimesheetRepository.delete(ligne);
    }

    @Transactional
    public TimesheetDto soumettreTimesheet(Long id, UserPrincipal currentUser) {
        return soumettreTimesheet(id, true, currentUser);
    }

    @Transactional
    public TimesheetDto soumettreTimesheet(Long id, boolean confirmed, UserPrincipal currentUser) {
        if (!confirmed) {
            throw new BusinessException("La soumission nécessite une confirmation explicite");
        }
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet non trouvée avec l'id : " + id));

        Freelance freelance = getFreelanceForUser(currentUser.getId());
        if (!timesheet.getFreelance().getId().equals(freelance.getId())) {
            throw new UnauthorizedException("Seul le freelance propriétaire peut soumettre sa timesheet");
        }

        if (timesheet.getStatut() != TimesheetStatut.BROUILLON && timesheet.getStatut() != TimesheetStatut.REJETEE) {
            throw new BusinessException("Seule une timesheet au statut BROUILLON ou REJETEE peut être soumise");
        }

        List<LigneTimesheet> lignes = ligneTimesheetRepository.findAllByTimesheetId(id);
        if (lignes.isEmpty()) {
            throw new BusinessException("Impossible de soumettre une timesheet vide. Veuillez saisir au moins une ligne d'activité.");
        }

        // Transition : BROUILLON / REJETEE -> SOUMISE (RG8)
        timesheet.setStatut(TimesheetStatut.SOUMISE);
        timesheet.setDateSoumission(LocalDateTime.now());
        timesheet.setMotifRejet(null); // Clear previous rejection reason if any

        Timesheet saved = timesheetRepository.save(timesheet);
        return toDetailedDto(saved);
    }

    @Transactional
    public TimesheetDto validerTimesheet(Long id, UserPrincipal currentUser) {
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet non trouvée avec l'id : " + id));

        if (currentUser.getRole() != Role.CHEF_PROJET) {
            throw new UnauthorizedException("Seul un chef de projet peut valider une timesheet");
        }

        if (currentUser.getRole() == Role.CHEF_PROJET && !isTimesheetManagedByChef(timesheet, currentUser.getId())) {
            throw new UnauthorizedException("Vous ne dirigez pas le projet concerné par cette timesheet");
        }

        if (timesheet.getStatut() != TimesheetStatut.SOUMISE) {
            throw new BusinessException("Seule une timesheet au statut SOUMISE peut être validée (statut actuel : " + timesheet.getStatut() + ")");
        }

        Utilisateur valideur = utilisateurRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        // Transition : SOUMISE -> VALIDEE (RG8)
        timesheet.setStatut(TimesheetStatut.VALIDEE);
        timesheet.setDateDecision(LocalDateTime.now());
        timesheet.setValidePar(valideur);

        List<LigneTimesheet> lignesValidees = ligneTimesheetRepository.findAllByTimesheetId(timesheet.getId());
        Timesheet savedTimesheet = timesheetRepository.save(timesheet);

        updateTaskProgress(savedTimesheet, lignesValidees);

        // Récupération du TJM appliqué depuis l'affectation active
        BigDecimal tjmApplique = determineTjmApplique(savedTimesheet);

        // Étape 11 & 12 : Dans la même transaction, génération du PV de réception (section 6.2 page 13)
        pvService.generatePv(savedTimesheet, tjmApplique);

        return toDetailedDto(savedTimesheet);
    }

    @Transactional
    public TimesheetDto rejeterTimesheet(Long id, RejectRequest req, UserPrincipal currentUser) {
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet non trouvée avec l'id : " + id));

        if (currentUser.getRole() != Role.CHEF_PROJET) {
            throw new UnauthorizedException("Seul un chef de projet peut rejeter une timesheet");
        }

        if (currentUser.getRole() == Role.CHEF_PROJET && !isTimesheetManagedByChef(timesheet, currentUser.getId())) {
            throw new UnauthorizedException("Vous ne dirigez pas le projet concerné par cette timesheet");
        }

        if (timesheet.getStatut() != TimesheetStatut.SOUMISE) {
            throw new BusinessException("Seule une timesheet au statut SOUMISE peut être rejetée");
        }

        if (req.getMotif() == null || req.getMotif().trim().isEmpty()) {
            throw new BusinessException("Le motif du rejet est obligatoire");
        }

        Utilisateur decisionPar = utilisateurRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        // Transition : SOUMISE -> REJETEE (RG8)
        timesheet.setStatut(TimesheetStatut.REJETEE);
        timesheet.setMotifRejet(req.getMotif().trim());
        timesheet.setDateDecision(LocalDateTime.now());
        timesheet.setValidePar(decisionPar);

        Timesheet saved = timesheetRepository.save(timesheet);
        return toDetailedDto(saved);
    }

    private Freelance getFreelanceForUser(Long userId) {
        return freelanceRepository.findByUtilisateurId(userId)
                .orElseThrow(() -> new BusinessException("Aucun profil freelance n'est associé à votre compte"));
    }

    private boolean isTimesheetManagedByChef(Timesheet t, Long chefProjetId) {
        List<LigneTimesheet> lignes = ligneTimesheetRepository.findAllByTimesheetId(t.getId());
        return isTimesheetManagedByChef(t, chefProjetId, lignes);
    }

    private boolean isTimesheetManagedByChef(Timesheet t, Long chefProjetId, List<LigneTimesheet> lignes) {
        if (!lignes.isEmpty()) {
            return lignes.stream()
                    .map(LigneTimesheet::getTache)
                    .filter(java.util.Objects::nonNull)
                    .map(Tache::getProjet)
                    .filter(java.util.Objects::nonNull)
                    .map(Projet::getChefProjet)
                    .filter(java.util.Objects::nonNull)
                    .allMatch(chef -> chef.getId().equals(chefProjetId))
                    && lignes.stream().allMatch(ligne -> ligne.getTache() != null
                        && ligne.getTache().getProjet() != null
                        && ligne.getTache().getProjet().getChefProjet() != null);
        }
        // Fallback: check active affectations of the freelance
        if (t.getFreelance() == null || t.getSemaineDebut() == null) {
            return false;
        }
        List<Affectation> affectations = affectationRepository.findActiveAffectationsByFreelanceAndDate(
                t.getFreelance().getId(), t.getSemaineDebut());
        return !affectations.isEmpty() && affectations.stream()
                .allMatch(a -> a.getProjet() != null
                        && a.getProjet().getChefProjet() != null
                        && a.getProjet().getChefProjet().getId().equals(chefProjetId));
    }

    private BigDecimal determineTjmApplique(Timesheet t) {
        List<LigneTimesheet> lignes = ligneTimesheetRepository.findAllByTimesheetId(t.getId());
        return determineTjmApplique(t, lignes);
    }

    private BigDecimal determineTjmApplique(Timesheet t, List<LigneTimesheet> lignes) {
        if (!lignes.isEmpty()) {
            Tache tache = lignes.get(0).getTache();
            if (tache != null && tache.getAffectation() != null && tache.getAffectation().getTjmApplique() != null) {
                return tache.getAffectation().getTjmApplique();
            }
        }
        List<Affectation> affectations = affectationRepository.findActiveAffectationsByFreelanceAndDate(
                t.getFreelance().getId(), t.getSemaineDebut());
        if (!affectations.isEmpty() && affectations.get(0).getTjmApplique() != null) {
            return affectations.get(0).getTjmApplique();
        }
        return t.getFreelance().getTjm();
    }

    private void updateTaskProgress(Timesheet timesheet, List<LigneTimesheet> lignesValidees) {
        lignesValidees.stream()
                .map(LigneTimesheet::getTache)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(tache -> {
                    BigDecimal chargeEstimeeHeures = tache.getHeuresPrevues();
                    if (chargeEstimeeHeures.compareTo(BigDecimal.ZERO) <= 0) {
                        return;
                    }

                    BigDecimal heuresValidees = ligneTimesheetRepository.findAllByTacheId(tache.getId()).stream()
                            .filter(ligne -> {
                                if (ligne.getTimesheet() != null
                                        && ligne.getTimesheet().getId() != null
                                        && ligne.getTimesheet().getId().equals(timesheet.getId())) {
                                    return true;
                                }
                                return ligne.getTimesheet() != null
                                        && ligne.getTimesheet().getStatut() == TimesheetStatut.VALIDEE;
                            })
                            .map(LigneTimesheet::getHeures)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int avancement = heuresValidees
                            .divide(chargeEstimeeHeures, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(0, RoundingMode.HALF_UP)
                            .min(BigDecimal.valueOf(100))
                            .intValue();
                    avancement = Math.max(tache.getAvancement() != null ? tache.getAvancement() : 0, avancement);

                    tache.setAvancement(avancement);
                    tache.setStatut(avancement >= 100
                            ? TacheStatut.TERMINEE
                            : avancement > 0 ? TacheStatut.EN_COURS : TacheStatut.A_FAIRE);
                    tacheRepository.save(tache);
                });
    }

    private TimesheetDto toDetailedDto(Timesheet t) {
        TimesheetDto dto = mapper.toTimesheetDto(t);
        List<LigneTimesheet> lignes = ligneTimesheetRepository.findAllByTimesheetId(t.getId());
        dto.setLignes(lignes.stream().map(mapper::toLigneTimesheetDto).collect(Collectors.toList()));

        BigDecimal totalHeures = lignes.stream()
                .map(LigneTimesheet::getHeures)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalHeuresSemaine(totalHeures);

        BigDecimal tjm = determineTjmApplique(t);
        BigDecimal jours = totalHeures.divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);
        dto.setMontantEstime(jours.multiply(tjm).setScale(2, RoundingMode.HALF_UP));

        pvReceptionRepository.findByTimesheetId(t.getId()).ifPresent(pv -> {
            dto.setPvId(pv.getId());
            dto.setNumeroPv(pv.getNumero());
        });

        return dto;
    }
}
