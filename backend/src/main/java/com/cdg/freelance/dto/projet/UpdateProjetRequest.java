package com.cdg.freelance.dto.projet;

import com.cdg.freelance.entity.ProjetStatut;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateProjetRequest {

    @NotBlank(message = "Le nom du projet est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "Le budget est obligatoire")
    @DecimalMin(value = "1.00", message = "Le budget doit être supérieur à 0")
    private BigDecimal budget;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin prévue est obligatoire")
    private LocalDate dateFinPrevue;

    private ProjetStatut statut;
    private Long typeProjetId;
    private Long chefProjetId;

    public UpdateProjetRequest() {}

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFinPrevue() {
        return dateFinPrevue;
    }

    public void setDateFinPrevue(LocalDate dateFinPrevue) {
        this.dateFinPrevue = dateFinPrevue;
    }

    public ProjetStatut getStatut() {
        return statut;
    }

    public void setStatut(ProjetStatut statut) {
        this.statut = statut;
    }

    public Long getTypeProjetId() {
        return typeProjetId;
    }

    public void setTypeProjetId(Long typeProjetId) {
        this.typeProjetId = typeProjetId;
    }

    public Long getChefProjetId() {
        return chefProjetId;
    }

    public void setChefProjetId(Long chefProjetId) {
        this.chefProjetId = chefProjetId;
    }
}
