package com.cdg.freelance.controller;

import com.cdg.freelance.dto.facturation.PaiementDto;
import com.cdg.freelance.dto.facturation.PvDto;
import com.cdg.freelance.dto.facturation.UpdatePaiementRequest;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.PaiementService;
import com.cdg.freelance.service.PvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Facturation", description = "Procès-Verbaux de réception (PV) et suivi des règlements fournisseurs")
public class FacturationController {

    private final PvService pvService;
    private final PaiementService paiementService;

    public FacturationController(PvService pvService, PaiementService paiementService) {
        this.pvService = pvService;
        this.paiementService = paiementService;
    }

    @GetMapping("/pv")
    @Operation(summary = "Lister les PV de réception selon le périmètre de l'utilisateur")
    public ResponseEntity<List<PvDto>> listPvs(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pvService.listPvs(principal));
    }

    @GetMapping("/pv/{id}")
    @Operation(summary = "Consulter le détail d'un PV de réception")
    public ResponseEntity<PvDto> getPv(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pvService.getPvById(id, principal));
    }

    @GetMapping("/pv/{id}/pdf")
    @Operation(summary = "Télécharger le document PDF officiel du PV de réception")
    public ResponseEntity<byte[]> downloadPvPdf(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        byte[] pdfData = pvService.getPdfBytes(id, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"PV-CDG-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @GetMapping("/paiements")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Lister et suivre les paiements des prestations freelances")
    public ResponseEntity<List<PaiementDto>> listPaiements(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paiementService.listPaiements(principal));
    }

    @PatchMapping("/paiements/{id}")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    @Operation(summary = "Mettre à jour le statut du paiement (ex: EN_ATTENTE -> PAYE)")
    public ResponseEntity<PaiementDto> updatePaiement(@PathVariable Long id, @Valid @RequestBody UpdatePaiementRequest req) {
        return ResponseEntity.ok(paiementService.updatePaiement(id, req));
    }
}
