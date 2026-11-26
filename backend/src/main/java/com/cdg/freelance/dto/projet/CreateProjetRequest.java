package com.cdg.freelance.dto.projet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateProjetRequest {

    @NotBlank(message = "Le code projet est obligatoire")
    private String code;

    @NotBlank(message = "Le nom du projet est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "Le budget est obligatoire")
    @DecimalMin(value = "1.00", message = "Le budget doit être supérieur à 0")
    private BigDecimal budget;

    @NotNull(message = "Les heures prévues sont obligatoires")
    @DecimalMin(value = "0.01", message = "Les heures prévues doivent être positives")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal heuresPrevues;

    @NotNull(message = "Le nombre de tâches prévues est obligatoire")
    @Min(value = 1, message = "Le nombre de tâches prévues doit être positif")
    private Integer nombreTachesPrevues;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    private LocalDate dateFinPrevue;

    @NotNull(message = "Le type de projet est obligatoire")
    private Long typeProjetId;

    @NotNull(message = "Le chef de projet est obligatoire")
    private Long chefProjetId;

    public CreateProjetRequest() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public BigDecimal getHeuresPrevues() { return heuresPrevues; }
    public void setHeuresPrevues(BigDecimal heuresPrevues) { this.heuresPrevues = heuresPrevues; }
    public Integer getNombreTachesPrevues() { return nombreTachesPrevues; }
    public void setNombreTachesPrevues(Integer nombreTachesPrevues) { this.nombreTachesPrevues = nombreTachesPrevues; }

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
