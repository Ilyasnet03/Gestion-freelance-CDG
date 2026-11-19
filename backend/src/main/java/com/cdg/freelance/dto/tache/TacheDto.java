package com.cdg.freelance.dto.tache;

import com.cdg.freelance.entity.TacheStatut;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TacheDto {

    private Long id;
    private Long projetId;
    private String projetNom;
    private Long affectationId;
    private Long freelanceId;
    private String freelanceNomComplet;
    private String titre;
    private BigDecimal chargeEstimee;
    private Integer avancement;
    private TacheStatut statut;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    public TacheDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjetId() {
        return projetId;
    }

    public void setProjetId(Long projetId) {
        this.projetId = projetId;
    }

    public String getProjetNom() {
        return projetNom;
    }

    public void setProjetNom(String projetNom) {
        this.projetNom = projetNom;
    }

    public Long getAffectationId() {
        return affectationId;
    }

    public void setAffectationId(Long affectationId) {
        this.affectationId = affectationId;
    }

    public Long getFreelanceId() {
        return freelanceId;
    }

    public void setFreelanceId(Long freelanceId) {
        this.freelanceId = freelanceId;
    }

    public String getFreelanceNomComplet() {
        return freelanceNomComplet;
    }

    public void setFreelanceNomComplet(String freelanceNomComplet) {
        this.freelanceNomComplet = freelanceNomComplet;
    }

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
