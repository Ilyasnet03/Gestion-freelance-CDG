package com.cdg.freelance.controller;

import com.cdg.freelance.dto.dashboard.DashboardKpiDto;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicateurs de pilotage, graphiques et KPI en temps réel")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpi")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN', 'FREELANCE')")
    @Operation(summary = "Obtenir les indicateurs, graphiques budgétaires et alertes récentes pour le profil connecté")
    public ResponseEntity<DashboardKpiDto> getKpi(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getDashboardKpi(principal));
    }
}
