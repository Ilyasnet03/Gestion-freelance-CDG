package com.cdg.freelance.controller;

import com.cdg.freelance.dto.alerte.AlerteDto;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.AlerteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertes")
@PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
@Tag(name = "Alertes", description = "Consultation et traitement des alertes émises par l'analyse prédictive")
public class AlerteController {

    private final AlerteService alerteService;

    public AlerteController(AlerteService alerteService) {
        this.alerteService = alerteService;
    }

    @GetMapping
    @Operation(summary = "Lister les alertes du portefeuille de projets")
    public ResponseEntity<List<AlerteDto>> listAlertes(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(alerteService.listAlertes(principal));
    }

    @PatchMapping("/{id}/traitee")
    @Operation(summary = "Marquer une alerte comme traitée par le chef de projet")
    public ResponseEntity<AlerteDto> markAsTraitee(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(alerteService.markAsTraitee(id, principal));
    }
}
