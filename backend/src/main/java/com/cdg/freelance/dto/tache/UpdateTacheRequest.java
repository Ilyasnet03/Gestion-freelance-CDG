package com.cdg.freelance.dto.tache;

import com.cdg.freelance.entity.TacheStatut;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateTacheRequest {

    @NotBlank(message = "Le titre de la tâche est obligatoire")
    private String titre;

    @NotNull(message = "La charge estimée est obligatoire")
    @DecimalMin(value = "0.0", message = "La charge estimée ne peut pas être négative")
    private BigDecimal chargeEstimee;

    @Min(value = 0, message = "L'avancement minimum est 0")
    @Max(value = 100, message = "L'avancement maximum est 100")
    private Integer avancement;

    private TacheStatut statut;
    private Long affectationId;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    public UpdateTacheRequest() {}

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

    public Integer getAvancement() {
        return avancement;
    }

    public void setAvancement(Integer avancement) {
        this.avancement = avancement;
    }

    public TacheStatut getStatut() {
        return statut;
    }

    public void setStatut(TacheStatut statut) {
        this.statut = statut;
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
