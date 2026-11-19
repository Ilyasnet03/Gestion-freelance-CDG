package com.cdg.freelance.controller;

import com.cdg.freelance.dto.admin.CreateUserRequest;
import com.cdg.freelance.dto.admin.ParametreDto;
import com.cdg.freelance.dto.admin.TypeProjetDto;
import com.cdg.freelance.dto.admin.UpdateUserRequest;
import com.cdg.freelance.dto.auth.UserDto;
import com.cdg.freelance.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Gestion des comptes, paramètres et types de projet")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/utilisateurs")
    @Operation(summary = "Lister tous les comptes utilisateurs")
    public ResponseEntity<List<UserDto>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PostMapping("/utilisateurs")
    @Operation(summary = "Créer un nouvel utilisateur")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(req));
    }

    @PutMapping("/utilisateurs/{id}")
    @Operation(summary = "Modifier un compte utilisateur et ses rôles")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(adminService.updateUser(id, req));
    }

    @PatchMapping("/utilisateurs/{id}/statut")
    @Operation(summary = "Activer ou désactiver un compte")
    public ResponseEntity<UserDto> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserStatus(id));
    }

    @GetMapping("/parametres")
    @Operation(summary = "Lister les paramètres du système")
    public ResponseEntity<List<ParametreDto>> listParametres() {
        return ResponseEntity.ok(adminService.listParametres());
    }

    @PutMapping("/parametres")
    @Operation(summary = "Modifier la valeur d'un paramètre")
    public ResponseEntity<ParametreDto> updateParametre(@Valid @RequestBody ParametreDto dto) {
        return ResponseEntity.ok(adminService.updateParametre(dto.getCle(), dto.getValeur()));
    }

    @GetMapping("/types-projet")
    @Operation(summary = "Lister les types de projets CDG")
    public ResponseEntity<List<TypeProjetDto>> listTypesProjet() {
        return ResponseEntity.ok(adminService.listTypesProjet());
    }

    @PostMapping("/types-projet")
    @Operation(summary = "Créer un nouveau type de projet")
    public ResponseEntity<TypeProjetDto> createTypeProjet(@Valid @RequestBody TypeProjetDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createTypeProjet(dto.getLibelle()));
    }
}
