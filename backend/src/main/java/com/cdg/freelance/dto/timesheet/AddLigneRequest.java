package com.cdg.freelance.dto.timesheet;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AddLigneRequest {

    @NotNull(message = "L'ID de la tâche est obligatoire")
    private Long tacheId;

    @NotNull(message = "La date de saisie est obligatoire")
    private LocalDate dateSaisie;

    @NotNull(message = "Le nombre d'heures est obligatoire")
    @DecimalMin(value = "0.01", message = "Le nombre d'heures doit être supérieur à 0")
    @DecimalMax(value = "10.00", message = "Le nombre d'heures par ligne ne peut dépasser 10 h")
    private BigDecimal heures;

    public AddLigneRequest() {}

    public AddLigneRequest(Long tacheId, LocalDate dateSaisie, BigDecimal heures) {
        this.tacheId = tacheId;
        this.dateSaisie = dateSaisie;
        this.heures = heures;
    }

    public Long getTacheId() {
        return tacheId;
    }

    public void setTacheId(Long tacheId) {
        this.tacheId = tacheId;
    }

    public LocalDate getDateSaisie() {
        return dateSaisie;
    }

    public void setDateSaisie(LocalDate dateSaisie) {
        this.dateSaisie = dateSaisie;
    }

    public BigDecimal getHeures() {
        return heures;
    }

    public void setHeures(BigDecimal heures) {
        this.heures = heures;
    }
}
