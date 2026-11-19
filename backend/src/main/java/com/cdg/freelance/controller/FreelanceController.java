package com.cdg.freelance.controller;

import com.cdg.freelance.dto.freelance.CreateFreelanceRequest;
import com.cdg.freelance.dto.freelance.FreelanceDto;
import com.cdg.freelance.dto.freelance.UpdateFreelanceRequest;
import com.cdg.freelance.security.UserPrincipal;
import com.cdg.freelance.service.FreelanceService;
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
@RequestMapping("/api/freelances")
@Tag(name = "Freelances", description = "Gestion des fiches de consultants freelances")
public class FreelanceController {

    private final FreelanceService freelanceService;

    public FreelanceController(FreelanceService freelanceService) {
        this.freelanceService = freelanceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    @Operation(summary = "Lister tous les freelances")
    public ResponseEntity<List<FreelanceDto>> listFreelances() {
        return ResponseEntity.ok(freelanceService.listFreelances());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'FREELANCE')")
    @Operation(summary = "Consulter le détail d'un freelance")
    public ResponseEntity<FreelanceDto> getFreelance(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(freelanceService.getFreelanceById(id, principal));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'FREELANCE')")
    @Operation(summary = "Consulter la fiche freelance d'un utilisateur")
    public ResponseEntity<FreelanceDto> getFreelanceByUserId(@PathVariable Long userId,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(freelanceService.getFreelanceByUserId(userId, principal));
    }

    @GetMapping("/email")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'FREELANCE')")
    @Operation(summary = "Consulter la fiche freelance par email utilisateur")
    public ResponseEntity<FreelanceDto> getFreelanceByEmail(@RequestParam String email,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(freelanceService.getFreelanceByEmail(email, principal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    @Operation(summary = "Créer une nouvelle fiche freelance")
    public ResponseEntity<FreelanceDto> createFreelance(@Valid @RequestBody CreateFreelanceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(freelanceService.createFreelance(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    @Operation(summary = "Mettre à jour une fiche freelance")
    public ResponseEntity<FreelanceDto> updateFreelance(@PathVariable Long id, @Valid @RequestBody UpdateFreelanceRequest req) {
        return ResponseEntity.ok(freelanceService.updateFreelance(id, req));
    }
}
