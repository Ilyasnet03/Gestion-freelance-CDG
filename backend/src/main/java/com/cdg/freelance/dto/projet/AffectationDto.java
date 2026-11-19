package com.cdg.freelance.dto.projet;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AffectationDto {

    private Long id;
    private Long projetId;
    private String projetNom;
    private Long freelanceId;
    private String freelanceNomComplet;
    private String freelanceSpecialite;
    private BigDecimal tjmApplique;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    public AffectationDto() {}

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

    public String getFreelanceSpecialite() {
        return freelanceSpecialite;
    }

    public void setFreelanceSpecialite(String freelanceSpecialite) {
        this.freelanceSpecialite = freelanceSpecialite;
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
