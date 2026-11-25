package com.cdg.freelance.dto.freelance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class UpdateFreelanceRequest {

    @NotNull(message = "Le TJM est obligatoire")
    @DecimalMin(value = "0.01", message = "Le TJM doit être supérieur à 0")
    private BigDecimal tjm;

    private String specialite;
    private String statut;

    @DecimalMin(value = "0.01", message = "Le seuil doit être supérieur à 0")
    @DecimalMax(value = "10.00", message = "Le seuil ne peut dépasser 10 h")
    @Digits(integer = 2, fraction = 2, message = "Le seuil doit avoir au maximum deux décimales")
    private BigDecimal seuilHeuresJour;

    public UpdateFreelanceRequest() {}

    public BigDecimal getTjm() {
        return tjm;
    }

    public void setTjm(BigDecimal tjm) {
        this.tjm = tjm;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public BigDecimal getSeuilHeuresJour() { return seuilHeuresJour; }
    public void setSeuilHeuresJour(BigDecimal seuilHeuresJour) { this.seuilHeuresJour = seuilHeuresJour; }
}
