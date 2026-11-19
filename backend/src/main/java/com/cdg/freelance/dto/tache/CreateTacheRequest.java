package com.cdg.freelance.dto.tache;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateTacheRequest {

    @NotBlank(message = "Le titre de la tâche est obligatoire")
    private String titre;

    @NotNull(message = "La charge estimée est obligatoire")
    @DecimalMin(value = "0.0", message = "La charge estimée ne peut pas être négative")
    private BigDecimal chargeEstimee;

    private Long affectationId; // Optional: can assign to a project affectation
    private LocalDate dateDebut;
    private LocalDate dateFin;

    public CreateTacheRequest() {}

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public BigDecimal getChargeEstimee() {
        return chargeEstimee;
    }

    public void setChargeEstimee(BigDecimal chargeEstimee) {
        this.chargeEstimee = chargeEstimee;
    }

    public Long getAffectationId() {
        return affectationId;
    }

    public void setAffectationId(Long affectationId) {
        this.affectationId = affectationId;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }
}
