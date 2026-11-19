package com.cdg.freelance.dto.projet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateAffectationRequest {

    @NotNull(message = "L'ID freelance est obligatoire")
    private Long freelanceId;

    @NotNull(message = "Le TJM appliqué est obligatoire")
    @DecimalMin(value = "0.01", message = "Le TJM doit être supérieur à 0")
    private BigDecimal tjmApplique;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    private LocalDate dateFin;

    public CreateAffectationRequest() {}

    public Long getFreelanceId() {
        return freelanceId;
    }

    public void setFreelanceId(Long freelanceId) {
        this.freelanceId = freelanceId;
    }

    public BigDecimal getTjmApplique() {
        return tjmApplique;
    }

    public void setTjmApplique(BigDecimal tjmApplique) {
        this.tjmApplique = tjmApplique;
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
