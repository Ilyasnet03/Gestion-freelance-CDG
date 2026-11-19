package com.cdg.freelance.controller;

import com.cdg.freelance.dto.timesheet.*;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.TimesheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/timesheets")
@Tag(name = "Timesheets", description = "Saisie, soumission, validation et rejet des feuilles de temps")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @GetMapping
    @Operation(summary = "Lister les timesheets selon le rôle et le filtre semaine (semaine=YYYY-MM-DD)")
    public ResponseEntity<List<TimesheetDto>> listTimesheets(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semaine,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(timesheetService.listTimesheets(semaine, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une timesheet et ses lignes")
    public ResponseEntity<TimesheetDto> getTimesheet(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(timesheetService.getTimesheetById(id, principal));
    }

    @PostMapping
    @PreAuthorize("hasRole('FREELANCE')")
    @Operation(summary = "Ouvrir ou créer la feuille de temps pour une semaine donnée (le lundi)")
    public ResponseEntity<TimesheetDto> openTimesheet(
            @Valid @RequestBody CreateTimesheetRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timesheetService.openOrCreateTimesheet(req.getSemaineDebut(), principal));
    }

    @PostMapping("/{id}/lignes")
    @PreAuthorize("hasRole('FREELANCE')")
    @Operation(summary = "Ajouter ou modifier une ligne de saisie d'heures (contrôle 24 h journalier)")
    public ResponseEntity<LigneTimesheetDto> addOrUpdateLigne(
            @PathVariable Long id,
            @Valid @RequestBody AddLigneRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timesheetService.addOrUpdateLigne(id, req, principal));
    }

    @DeleteMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasRole('FREELANCE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une ligne de saisie de la feuille de temps")
    public void deleteLigne(
            @PathVariable Long id,
            @PathVariable Long ligneId,
            @AuthenticationPrincipal UserPrincipal principal) {
        timesheetService.deleteLigne(id, ligneId, principal);
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasRole('FREELANCE')")
    @Operation(summary = "Soumettre la feuille de temps (BROUILLON/REJETEE -> SOUMISE)")
    public ResponseEntity<TimesheetDto> soumettre(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(timesheetService.soumettreTimesheet(id, principal));
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Operation(summary = "Valider la timesheet et générer automatiquement le PV de réception en PDF")
    public ResponseEntity<TimesheetDto> valider(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(timesheetService.validerTimesheet(id, principal));
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    @Operation(summary = "Rejeter la timesheet avec motif obligatoire (SOUMISE -> REJETEE)")
    public ResponseEntity<TimesheetDto> rejeter(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(timesheetService.rejeterTimesheet(id, req, principal));
    }
}
