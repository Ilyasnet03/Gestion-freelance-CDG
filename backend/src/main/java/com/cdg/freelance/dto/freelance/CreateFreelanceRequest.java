package com.cdg.freelance.dto.freelance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateFreelanceRequest {

    @NotNull(message = "L'ID utilisateur est obligatoire")
    private Long utilisateurId;

    @NotNull(message = "Le TJM est obligatoire")
    @DecimalMin(value = "0.01", message = "Le TJM doit être supérieur à 0")
    private BigDecimal tjm;

    private String specialite;
    private String statut = "ACTIF";

    public CreateFreelanceRequest() {}

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

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
}
