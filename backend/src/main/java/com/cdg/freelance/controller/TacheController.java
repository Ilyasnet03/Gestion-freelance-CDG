package com.cdg.freelance.controller;

import com.cdg.freelance.dto.tache.CreateTacheRequest;
import com.cdg.freelance.dto.tache.TacheDto;
import com.cdg.freelance.dto.tache.UpdateAvancementRequest;
import com.cdg.freelance.dto.tache.UpdateTacheRequest;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.ProjetService;
import com.cdg.freelance.service.TacheService;
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
@RequestMapping("/api")
@Tag(name = "Tâches", description = "Gestion des tâches et avancement des livrables")
public class TacheController {

    private final TacheService tacheService;
    private final ProjetService projetService;

    public TacheController(TacheService tacheService, ProjetService projetService) {
        this.tacheService = tacheService;
        this.projetService = projetService;
    }

    @GetMapping("/projets/{id}/taches")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Lister les tâches d'un projet")
    public ResponseEntity<List<TacheDto>> listTachesByProjet(@PathVariable Long id,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        projetService.ensureProjectAccess(id, principal);
        return ResponseEntity.ok(tacheService.listTachesByProjet(id));
    }

    @GetMapping("/mes-missions")
    @PreAuthorize("hasRole('FREELANCE')")
    @Operation(summary = "Lister les tâches affectées au freelance connecté")
    public ResponseEntity<List<TacheDto>> listMesMissions(@RequestParam(defaultValue = "false") boolean disponibles,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tacheService.listMesMissions(principal, disponibles));
    }

    @PostMapping("/projets/{id}/taches")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Operation(summary = "Créer une nouvelle tâche dans un projet")
    public ResponseEntity<TacheDto> createTache(@PathVariable Long id, @Valid @RequestBody CreateTacheRequest req,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        projetService.ensureProjectAccess(id, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(tacheService.createTache(id, req));
    }

    @GetMapping("/taches/{id}")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Consulter le détail d'une tâche")
    public ResponseEntity<TacheDto> getTache(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        TacheDto tache = tacheService.getTacheById(id);
        projetService.ensureProjectAccess(tache.getProjetId(), principal);
        return ResponseEntity.ok(tache);
    }

    @PutMapping("/taches/{id}")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Operation(summary = "Mettre à jour une tâche")
    public ResponseEntity<TacheDto> updateTache(@PathVariable Long id, @Valid @RequestBody UpdateTacheRequest req,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        TacheDto tache = tacheService.getTacheById(id);
        projetService.ensureProjectAccess(tache.getProjetId(), principal);
        return ResponseEntity.ok(tacheService.updateTache(id, req));
    }

    @PatchMapping("/taches/{id}/avancement")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Operation(summary = "Mettre à jour le pourcentage d'avancement d'une tâche (0 à 100%)")
    public ResponseEntity<TacheDto> updateAvancement(@PathVariable Long id, @Valid @RequestBody UpdateAvancementRequest req,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        TacheDto tache = tacheService.getTacheById(id);
        projetService.ensureProjectAccess(tache.getProjetId(), principal);
        return ResponseEntity.ok(tacheService.updateAvancement(id, req, principal));
    }
}
