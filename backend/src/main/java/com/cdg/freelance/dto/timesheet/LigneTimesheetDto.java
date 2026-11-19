package com.cdg.freelance.dto.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LigneTimesheetDto {

    private Long id;
    private Long timesheetId;
    private Long tacheId;
    private String tacheTitre;
    private Long projetId;
    private String projetNom;
    private LocalDate dateSaisie;
    private BigDecimal heures;

    public LigneTimesheetDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTimesheetId() {
        return timesheetId;
    }

    public void setTimesheetId(Long timesheetId) {
        this.timesheetId = timesheetId;
    }

    public Long getTacheId() {
        return tacheId;
    }

    public void setTacheId(Long tacheId) {
        this.tacheId = tacheId;
    }

    public String getTacheTitre() {
        return tacheTitre;
    }

    public void setTacheTitre(String tacheTitre) {
        this.tacheTitre = tacheTitre;
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
