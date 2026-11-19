package com.cdg.freelance.controller;

import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.AnalyseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/analyses")
@PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
@Tag(name = "Analyses & Aide à la décision", description = "Lancement des analyses FastAPI et génération du rapport PDF mensuel")
public class AnalyseProxyController {

    private final AnalyseService analyseService;

    public AnalyseProxyController(AnalyseService analyseService) {
        this.analyseService = analyseService;
    }

    @PostMapping("/executer")
    @Operation(summary = "Déclencher immédiatement l'analyse des budgets, retards et feuilles de temps")
    public ResponseEntity<Map<String, Object>> executeAnalyses() {
        return ResponseEntity.ok(analyseService.runDailyAnalysis());
    }

    @GetMapping("/rapport-mensuel")
    @Operation(summary = "Télécharger le rapport analytique mensuel consolidé en PDF")
    public ResponseEntity<byte[]> getRapportMensuel(@RequestParam(required = false) String mois,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        YearMonth month;
        try {
            month = mois == null || mois.isBlank() ? YearMonth.now() : YearMonth.parse(mois);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Le mois doit respecter le format AAAA-MM");
        }
        byte[] pdfBytes = analyseService.generateMonthlyReportPdf(month, principal);
        String filename = "Rapport-Mensuel-CDG-" + month + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
