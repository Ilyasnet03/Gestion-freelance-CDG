package com.cdg.freelance.service;

import com.cdg.freelance.dto.alerte.AlerteDto;
import com.cdg.freelance.dto.projet.ProjetDto;
import com.cdg.freelance.entity.*;
import com.cdg.freelance.exception.BusinessException;
import com.cdg.freelance.repository.AlerteRepository;
import com.cdg.freelance.repository.LigneTimesheetRepository;
import com.cdg.freelance.repository.ParametreRepository;
import com.cdg.freelance.repository.ProjetRepository;
import com.cdg.freelance.repository.TimesheetRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnalyseService {

    private final ProjetService projetService;
    private final TimesheetRepository timesheetRepository;
    private final LigneTimesheetRepository ligneTimesheetRepository;
    private final AlerteRepository alerteRepository;
    private final ParametreRepository parametreRepository;
    private final AlerteService alerteService;
    private final ProjetRepository projetRepository;

    public AnalyseService(ProjetService projetService,
                         TimesheetRepository timesheetRepository,
                         LigneTimesheetRepository ligneTimesheetRepository,
                         AlerteRepository alerteRepository,
                         ParametreRepository parametreRepository,
                         AlerteService alerteService,
                         ProjetRepository projetRepository) {
        this.projetService = projetService;
        this.timesheetRepository = timesheetRepository;
        this.ligneTimesheetRepository = ligneTimesheetRepository;
        this.alerteRepository = alerteRepository;
        this.parametreRepository = parametreRepository;
        this.alerteService = alerteService;
        this.projetRepository = projetRepository;
    }

    @Transactional
    public Map<String, Object> runDailyAnalysis() {
        return runDailyAnalysisForProjects(projetRepository.findAll());
    }

    @Transactional
    public Map<String, Object> runDailyAnalysis(UserPrincipal currentUser) {
        List<Projet> projets;
        if (currentUser.getRole() == Role.ADMIN) {
            projets = projetRepository.findAll();
        } else if (currentUser.getRole() == Role.CHEF_PROJET) {
            projets = projetRepository.findAllByChefProjetId(currentUser.getId());
        } else {
            throw new AccessDeniedException("Seuls les chefs de projet et administrateurs peuvent lancer les analyses");
        }
        return runDailyAnalysisForProjects(projets);
    }

    private Map<String, Object> runDailyAnalysisForProjects(List<Projet> projets) {

        int attentionBudget = readThreshold("SEUIL_BUDGET_ATTENTION", 80);
        int criticalBudget = readThreshold("SEUIL_BUDGET_CRITIQUE", 100);
        int attentionDelay = readThreshold("SEUIL_RETARD_ATTENTION", 15);
        int criticalDelay = readThreshold("SEUIL_RETARD_CRITIQUE", 30);
        int validationDelayDays = readThreshold("DELAI_VALIDATION_JOURS", 3);
        BigDecimal dailyHoursInfo = BigDecimal.valueOf(readThreshold("MAX_HEURES_JOUR_INFO", 10));
        BigDecimal weeklyHoursInfo = BigDecimal.valueOf(readThreshold("MAX_HEURES_SEMAINE_INFO", 48));
        LocalDate today = LocalDate.now();

        int createdOrUpdated = 0;
        int resolved = 0;

        for (Projet projet : projets) {
            if (projet.getStatut() == ProjetStatut.TERMINE) {
                resolved += resolveAlert(projet, AlerteType.DEPASSEMENT_BUDGET);
                resolved += resolveAlert(projet, AlerteType.RETARD_PROJET);
                resolved += resolveAlert(projet, AlerteType.TIMESHEET_EN_RETARD);
                continue;
            }

            BigDecimal budget = projet.getBudget() == null ? BigDecimal.ZERO : projet.getBudget();
            double budgetRate = budget.signum() > 0
                    ? projetService.calculateBudgetConsomme(projet.getId())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(budget, 2, java.math.RoundingMode.HALF_UP)
                        .doubleValue()
                    : 0.0;
            AlerteNiveau budgetLevel = budgetRate >= criticalBudget ? AlerteNiveau.CRITIQUE
                    : budgetRate >= attentionBudget ? AlerteNiveau.ATTENTION : null;
            createdOrUpdated += syncAlert(projet, AlerteType.DEPASSEMENT_BUDGET, budgetLevel,
                    "Consommation budgétaire à " + String.format(java.util.Locale.ROOT, "%.2f", budgetRate)
                            + " % (seuil d'attention : " + attentionBudget + " %).");

            double scheduleGap = projetService.getAvancementAndBudget(projet.getId()).getEcartAvancement();
            AlerteNiveau delayLevel = scheduleGap >= criticalDelay ? AlerteNiveau.CRITIQUE
                    : scheduleGap >= attentionDelay ? AlerteNiveau.ATTENTION : null;
            createdOrUpdated += syncAlert(projet, AlerteType.RETARD_PROJET, delayLevel,
                    "Retard d'avancement de " + String.format(java.util.Locale.ROOT, "%.2f", scheduleGap)
                            + " points par rapport au calendrier.");
        }

        Map<Long, Long> overdueTimesheetsByProject = new HashMap<>();
        for (Timesheet timesheet : timesheetRepository.findAllByStatut(TimesheetStatut.SOUMISE)) {
            if (timesheet.getDateSoumission() == null
                    || ChronoUnit.DAYS.between(timesheet.getDateSoumission().toLocalDate(), today) < validationDelayDays
                    || timesheet.getLignes() == null) {
                continue;
            }

            timesheet.getLignes().stream()
                    .map(LigneTimesheet::getTache)
                    .filter(java.util.Objects::nonNull)
                    .map(Tache::getProjet)
                    .filter(java.util.Objects::nonNull)
                    .map(Projet::getId)
                    .distinct()
                    .forEach(projectId -> overdueTimesheetsByProject.merge(
                            projectId,
                            ChronoUnit.DAYS.between(timesheet.getDateSoumission().toLocalDate(), today),
                            Math::max));
        }

        for (Projet projet : projets) {
            if (projet.getStatut() == ProjetStatut.TERMINE) {
                resolved += resolveAlert(projet, AlerteType.TIMESHEET_EN_RETARD);
                continue;
            }
            Long oldestDelay = overdueTimesheetsByProject.get(projet.getId());
            AlerteNiveau level = oldestDelay == null ? null
                    : oldestDelay >= validationDelayDays * 2L ? AlerteNiveau.CRITIQUE : AlerteNiveau.ATTENTION;
            createdOrUpdated += syncAlert(projet, AlerteType.TIMESHEET_EN_RETARD, level,
                    oldestDelay == null ? null : "Une ou plusieurs feuilles de temps sont en attente depuis "
                            + oldestDelay + " jours.");
        }

        Map<FreelanceDay, BigDecimal> hoursByFreelanceDay = new HashMap<>();
        Map<FreelanceDay, Map<Long, Projet>> projectsByFreelanceDay = new HashMap<>();
        Map<FreelanceWeek, BigDecimal> hoursByFreelanceWeek = new HashMap<>();
        Map<FreelanceWeek, Map<Long, Projet>> projectsByFreelanceWeek = new HashMap<>();
        for (LigneTimesheet line : ligneTimesheetRepository.findAll()) {
            if (line.getTimesheet() == null || line.getTimesheet().getFreelance() == null
                    || line.getDateSaisie() == null || line.getDateSaisie().isBefore(today.minusDays(6))
                    || line.getTache() == null || line.getTache().getProjet() == null) {
                continue;
            }
            Long freelanceId = line.getTimesheet().getFreelance().getId();
            Projet project = line.getTache().getProjet();
            FreelanceDay day = new FreelanceDay(freelanceId, line.getDateSaisie());
            hoursByFreelanceDay.merge(day, line.getHeures(), BigDecimal::add);
            projectsByFreelanceDay.computeIfAbsent(day, ignored -> new HashMap<>()).put(project.getId(), project);

            FreelanceWeek week = new FreelanceWeek(freelanceId, line.getTimesheet().getSemaineDebut());
            hoursByFreelanceWeek.merge(week, line.getHeures(), BigDecimal::add);
            projectsByFreelanceWeek.computeIfAbsent(week, ignored -> new HashMap<>()).put(project.getId(), project);
        }

        Map<Long, BigDecimal> dailyAnomalies = new HashMap<>();
        hoursByFreelanceDay.forEach((day, hours) -> {
            if (hours.compareTo(dailyHoursInfo) > 0) {
                projectsByFreelanceDay.get(day).forEach((projectId, project) ->
                        dailyAnomalies.merge(projectId, hours, BigDecimal::max));
            }
        });
        Map<Long, BigDecimal> weeklyOverloads = new HashMap<>();
        hoursByFreelanceWeek.forEach((week, hours) -> {
            if (hours.compareTo(weeklyHoursInfo) > 0) {
                projectsByFreelanceWeek.get(week).forEach((projectId, project) ->
                        weeklyOverloads.merge(projectId, hours, BigDecimal::max));
            }
        });

        for (Projet projet : projets) {
            if (projet.getStatut() == ProjetStatut.TERMINE) {
                resolved += resolveAlert(projet, AlerteType.ANOMALIE_SAISIE);
                resolved += resolveAlert(projet, AlerteType.SURCHARGE_FREELANCE);
                continue;
            }
            BigDecimal dailyHours = dailyAnomalies.get(projet.getId());
            AlerteNiveau anomalyLevel = dailyHours == null ? null : AlerteNiveau.ATTENTION;
            createdOrUpdated += syncAlert(projet, AlerteType.ANOMALIE_SAISIE, anomalyLevel,
                    dailyHours == null ? null : "Saisie journaliere inhabituelle : " + dailyHours
                            + " h (seuil d'information : " + dailyHoursInfo + " h).");

            BigDecimal weeklyHours = weeklyOverloads.get(projet.getId());
            AlerteNiveau overloadLevel = weeklyHours == null ? null
                    : weeklyHours.compareTo(weeklyHoursInfo.add(BigDecimal.valueOf(8))) > 0
                        ? AlerteNiveau.CRITIQUE : AlerteNiveau.ATTENTION;
            createdOrUpdated += syncAlert(projet, AlerteType.SURCHARGE_FREELANCE, overloadLevel,
                    weeklyHours == null ? null : "Charge hebdomadaire elevee : " + weeklyHours
                            + " h (seuil d'information : " + weeklyHoursInfo + " h).");
        }

        return Map.of(
                "dateAnalyse", today.toString(),
                "projetsAnalyses", projets.size(),
                "alertesCreeesOuActualisees", createdOrUpdated,
                "alertesResolues", resolved
        );
    }

    @Transactional(readOnly = true)
    public byte[] generateMonthlyReportPdf(YearMonth month, UserPrincipal currentUser) {
        List<ProjetDto> projects = projetService.listProjets(currentUser);
        List<AlerteDto> monthAlerts = alerteService.listAlertes(currentUser).stream()
                .filter(alert -> alert.getDateCreation() != null
                        && YearMonth.from(alert.getDateCreation()).equals(month))
                .collect(Collectors.toList());
        BigDecimal totalBudget = projects.stream()
                .map(ProjetDto::getBudget)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumedBudget = projects.stream()
                .map(ProjetDto::getBudgetConsomme)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MonthlyReportPdf.render(month, currentUser.getRole().name(), projects, monthAlerts,
                totalBudget, consumedBudget);
    }

    private int readThreshold(String key, int fallback) {
        Optional<Parametre> parameter = parametreRepository.findByCle(key);
        if (parameter.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(parameter.get().getValeur());
        } catch (NumberFormatException exception) {
            throw new BusinessException("Valeur non numerique pour le parametre " + key);
        }
    }

    private int syncAlert(Projet projet, AlerteType type, AlerteNiveau level, String message) {
        Optional<Alerte> existing = alerteRepository.findByProjetIdAndTypeAndTraitee(projet.getId(), type, false);
        if (level == null) {
            if (existing.isEmpty()) {
                return 0;
            }
            Alerte alert = existing.get();
            alert.setTraitee(true);
            alerteRepository.save(alert);
            return 1;
        }

        if (existing.isPresent()) {
            Alerte alert = existing.get();
            boolean changed = alert.getNiveau() != level || !java.util.Objects.equals(alert.getMessage(), message);
            if (changed) {
                alert.setNiveau(level);
                alert.setMessage(message);
                alerteRepository.save(alert);
            }
            return changed ? 1 : 0;
        }

        Alerte alert = new Alerte();
        alert.setProjet(projet);
        alert.setType(type);
        alert.setNiveau(level);
        alert.setMessage(message);
        alert.setDateCreation(java.time.LocalDateTime.now());
        alert.setTraitee(false);
        alerteRepository.save(alert);
        return 1;
    }

    private int resolveAlert(Projet projet, AlerteType type) {
        Optional<Alerte> existing = alerteRepository.findByProjetIdAndTypeAndTraitee(projet.getId(), type, false);
        if (existing.isEmpty()) {
            return 0;
        }
        Alerte alert = existing.get();
        alert.setTraitee(true);
        alerteRepository.save(alert);
        return 1;
    }

    private record FreelanceDay(Long freelanceId, LocalDate date) {}

    private record FreelanceWeek(Long freelanceId, LocalDate weekStart) {}
}
