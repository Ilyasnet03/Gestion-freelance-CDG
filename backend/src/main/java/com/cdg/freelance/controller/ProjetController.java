package com.cdg.freelance.controller;

import com.cdg.freelance.dto.projet.*;
import com.cdg.freelance.dto.admin.TypeProjetDto;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.ProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projets")
@Tag(name = "Projets", description = "Gestion des projets, affectations et suivi budgétaire")
public class ProjetController {

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Lister les projets accessibles à l'utilisateur")
    public ResponseEntity<List<ProjetDto>> listProjets(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(projetService.listProjets(principal));
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Lister les types de projet disponibles")
    public ResponseEntity<List<TypeProjetDto>> listTypesProjet() {
        return ResponseEntity.ok(projetService.listTypesProjet());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Consulter le détail d'un projet")
    public ResponseEntity<ProjetDto> getProjet(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(projetService.getProjetById(id, principal));
    }

    @PostMapping
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Operation(summary = "Créer un nouveau projet")
    public ResponseEntity<ProjetDto> createProjet(@Valid @RequestBody CreateProjetRequest req,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetService.createProjet(req, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Operation(summary = "Modifier les informations d'un projet")
    public ResponseEntity<ProjetDto> updateProjet(@PathVariable Long id, @Valid @RequestBody UpdateProjetRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(projetService.updateProjet(id, req, principal));
    }

    @PostMapping("/{id}/affectations")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Operation(summary = "Affecter un freelance à un projet avec son TJM négocié")
    public ResponseEntity<AffectationDto> addAffectation(@PathVariable Long id, @Valid @RequestBody CreateAffectationRequest req,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetService.addAffectation(id, req, principal));
    }

    @GetMapping("/{id}/affectations")
    @Operation(summary = "Lister les affectations d'un projet")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<List<AffectationDto>> listAffectations(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(projetService.listAffectationsByProjet(id, principal));
    }

    @GetMapping("/{id}/avancement")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Consulter le suivi d'avancement et la consommation du budget du projet")
    public ResponseEntity<ProjetAvancementDto> getAvancement(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(projetService.getAvancementAndBudget(id, principal));
    }
}
