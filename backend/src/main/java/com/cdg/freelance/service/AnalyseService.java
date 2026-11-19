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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
        List<Projet> projets = projetRepository.findAll();

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

        List<String> lines = new ArrayList<>();
        lines.add("RAPPORT MENSUEL DE PILOTAGE - CDG");
        lines.add("Periode : " + month);
        lines.add("Genere le : " + LocalDate.now());
        lines.add("Profil : " + currentUser.getRole());
        lines.add("");
        lines.add("SYNTHESE");
        lines.add("Projets visibles : " + projects.size());
        lines.add("Budget total : " + totalBudget + " MAD");
        lines.add("Budget consomme : " + consumedBudget + " MAD");
        lines.add("Alertes emises durant le mois : " + monthAlerts.size());
        lines.add("");
        lines.add("DETAIL DES PROJETS");
        for (ProjetDto project : projects) {
            lines.add(project.getCode() + " - " + project.getNom());
            lines.add("  Statut : " + project.getStatut() + " | Avancement : "
                    + (project.getAvancementGlobal() == null ? 0 : project.getAvancementGlobal()) + " %");
            lines.add("  Budget : " + project.getBudgetConsomme() + " / " + project.getBudget() + " MAD");
        }
        lines.add("");
        lines.add("ALERTES DU MOIS");
        if (monthAlerts.isEmpty()) {
            lines.add("Aucune alerte emise durant cette periode.");
        } else {
            monthAlerts.forEach(alert -> lines.add(alert.getNiveau() + " | " + alert.getProjetCode()
                    + " | " + alert.getMessage()));
        }
        return createPdf(lines);
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
            return resolveAlert(existing);
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
        return resolveAlert(alerteRepository.findByProjetIdAndTypeAndTraitee(projet.getId(), type, false));
    }

    private int resolveAlert(Optional<Alerte> alert) {
        if (alert.isEmpty()) {
            return 0;
        }
        alert.get().setTraitee(true);
        alerteRepository.save(alert.get());
        return 1;
    }

    private byte[] createPdf(List<String> lines) {
        try {
            List<List<String>> pages = new ArrayList<>();
            for (int start = 0; start < lines.size(); start += 48) {
                pages.add(lines.subList(start, Math.min(start + 48, lines.size())));
            }
            if (pages.isEmpty()) {
                pages.add(List.of("Rapport mensuel CDG"));
            }

            int fontObject = 3 + pages.size() * 2;
            StringBuilder kids = new StringBuilder();
            for (int page = 0; page < pages.size(); page++) {
                int pageObject = 3 + page * 2;
                kids.append(pageObject).append(" 0 R ");
            }

            List<byte[]> objects = new ArrayList<>();
            objects.add(ascii("<< /Type /Catalog /Pages 2 0 R >>"));
            objects.add(ascii("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>"));
            for (int page = 0; page < pages.size(); page++) {
                int pageObject = 3 + page * 2;
                int contentObject = pageObject + 1;
                objects.add(ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                        + "/Contents " + contentObject + " 0 R /Resources << /Font << /F1 "
                        + fontObject + " 0 R >> >> >>"));
                StringBuilder content = new StringBuilder("BT\n/F1 10 Tf\n50 790 Td\n14 TL\n");
                for (String line : pages.get(page)) {
                    content.append('(').append(pdfText(line)).append(") Tj\nT*\n");
                }
                content.append("ET\n");
                byte[] stream = ascii(content.toString());
                ByteArrayOutputStream streamObject = new ByteArrayOutputStream();
                streamObject.write(ascii("<< /Length " + stream.length + " >>\nstream\n"));
                streamObject.write(stream);
                streamObject.write(ascii("endstream"));
                objects.add(streamObject.toByteArray());
            }
            objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));

            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            pdf.write(ascii("%PDF-1.4\n"));
            List<Integer> offsets = new ArrayList<>();
            for (int index = 0; index < objects.size(); index++) {
                offsets.add(pdf.size());
                pdf.write(ascii((index + 1) + " 0 obj\n"));
                pdf.write(objects.get(index));
                pdf.write(ascii("\nendobj\n"));
            }

            int xrefOffset = pdf.size();
            pdf.write(ascii("xref\n0 " + (objects.size() + 1) + "\n"));
            pdf.write(ascii("0000000000 65535 f \n"));
            for (Integer offset : offsets) {
                pdf.write(ascii(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offset)));
            }
            pdf.write(ascii("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n"
                    + "startxref\n" + xrefOffset + "\n%%EOF\n"));
            return pdf.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("Impossible de generer le rapport PDF : " + exception.getMessage());
        }
    }

    private byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private record FreelanceDay(Long freelanceId, LocalDate date) {}

    private record FreelanceWeek(Long freelanceId, LocalDate weekStart) {}

    private String pdfText(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\x20-\\x7E]", "?");
        return normalized.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
