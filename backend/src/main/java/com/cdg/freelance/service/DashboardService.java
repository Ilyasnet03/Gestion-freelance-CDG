package com.cdg.freelance.service;

import com.cdg.freelance.dto.dashboard.DashboardKpiDto;
import com.cdg.freelance.dto.projet.ProjetAvancementDto;
import com.cdg.freelance.dto.projet.ProjetDto;
import com.cdg.freelance.entity.AlerteNiveau;
import com.cdg.freelance.entity.ProjetStatut;
import com.cdg.freelance.entity.TimesheetStatut;
import com.cdg.freelance.mapper.EntityMapper;
import com.cdg.freelance.repository.AlerteRepository;
import com.cdg.freelance.repository.TimesheetRepository;
import com.cdg.freelance.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProjetService projetService;
    private final TimesheetRepository timesheetRepository;
    private final AlerteRepository alerteRepository;
    private final AlerteService alerteService;
    private final EntityMapper mapper;

    public DashboardService(ProjetService projetService,
                            TimesheetRepository timesheetRepository,
                            AlerteRepository alerteRepository,
                            AlerteService alerteService,
                            EntityMapper mapper) {
        this.projetService = projetService;
        this.timesheetRepository = timesheetRepository;
        this.alerteRepository = alerteRepository;
        this.alerteService = alerteService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DashboardKpiDto getDashboardKpi(UserPrincipal currentUser) {
        if (currentUser.getRole() != com.cdg.freelance.entity.Role.ADMIN
                && currentUser.getRole() != com.cdg.freelance.entity.Role.CHEF_PROJET) {
            throw new AccessDeniedException("Le tableau de bord est réservé aux chefs de projet et administrateurs");
        }
        DashboardKpiDto dto = new DashboardKpiDto();

        List<ProjetDto> userProjets = projetService.listProjets(currentUser);

        // 1. Projets Actifs
        long actifsCount = userProjets.stream()
                .filter(p -> p.getStatut() == ProjetStatut.EN_COURS || p.getStatut() == ProjetStatut.PLANIFIE)
                .count();
        dto.setProjetsActifsCount(actifsCount);

        // 2. Budget global & budget consommé
        BigDecimal totalBudget = userProjets.stream()
                .map(ProjetDto::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setBudgetGlobalTotal(totalBudget);

        BigDecimal totalConsomme = userProjets.stream()
                .map(ProjetDto::getBudgetConsomme)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setBudgetConsommeTotal(totalConsomme);

        // 3. Timesheets à valider (statut SOUMISE)
        long tsAValider;
        if (currentUser.getRole() == com.cdg.freelance.entity.Role.CHEF_PROJET) {
            tsAValider = timesheetRepository.findByStatutAndChefProjetId(TimesheetStatut.SOUMISE, currentUser.getId()).size();
        } else {
            tsAValider = timesheetRepository.findAllByStatut(TimesheetStatut.SOUMISE).size();
        }
        dto.setTimesheetsAValiderCount(tsAValider);

        // 4. Alertes critiques & attention
        var userAlertes = alerteService.listAlertes(currentUser);
        long critiques = userAlertes.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getTraitee()) && a.getNiveau() == AlerteNiveau.CRITIQUE)
                .count();
        long attention = userAlertes.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getTraitee()) && a.getNiveau() == AlerteNiveau.ATTENTION)
                .count();
        dto.setAlertesCritiquesCount(critiques);
        dto.setAlertesAttentionCount(attention);

        // 5. Graphique de consommation par projet
        List<DashboardKpiDto.ProjetBudgetChartItem> budgetItems = userProjets.stream().map(p ->
                new DashboardKpiDto.ProjetBudgetChartItem(
                        p.getCode(),
                        p.getNom(),
                        p.getBudget(),
                        p.getBudgetConsomme(),
                        p.getTauxConsommation()
                )
        ).collect(Collectors.toList());
        dto.setConsommationBudgetProjets(budgetItems);

        // 6. Graphique d'avancement réel vs attendu par projet
        List<DashboardKpiDto.ProjetAvancementChartItem> avancementItems = userProjets.stream().map(p -> {
            ProjetAvancementDto av = projetService.getAvancementAndBudget(p.getId());
            return new DashboardKpiDto.ProjetAvancementChartItem(
                    p.getCode(),
                    p.getNom(),
                    av.getAvancementReel(),
                    av.getAvancementAttendu()
            );
        }).collect(Collectors.toList());
        dto.setAvancementProjets(avancementItems);

        // 7. Dernières alertes non traitées
        dto.setDernieresAlertes(
                userAlertes.stream()
                        .filter(a -> !Boolean.TRUE.equals(a.getTraitee()))
                        .limit(5)
                        .collect(Collectors.toList())
        );

        return dto;
    }
}
