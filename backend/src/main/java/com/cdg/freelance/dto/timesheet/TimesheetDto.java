package com.cdg.freelance.dto.timesheet;

import com.cdg.freelance.entity.TimesheetStatut;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TimesheetDto {

    private Long id;
    private Long freelanceId;
    private String freelanceNomComplet;
    private LocalDate semaineDebut;
    private TimesheetStatut statut;
    private String motifRejet;
    private LocalDateTime dateSoumission;
    private LocalDateTime dateDecision;
    private Long valideParId;
    private String valideParNomComplet;

    private Long projetId;
    private String projetNom;

    private BigDecimal totalHeuresSemaine = BigDecimal.ZERO;
    private BigDecimal montantEstime = BigDecimal.ZERO;

    private Long pvId;
    private String numeroPv;

    private List<LigneTimesheetDto> lignes = new ArrayList<>();

    public TimesheetDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getSemaineDebut() {
        return semaineDebut;
    }

    public void setSemaineDebut(LocalDate semaineDebut) {
        this.semaineDebut = semaineDebut;
    }

    public TimesheetStatut getStatut() {
        return statut;
    }

    public void setStatut(TimesheetStatut statut) {
        this.statut = statut;
    }

    public String getMotifRejet() {
        return motifRejet;
    }

    public void setMotifRejet(String motifRejet) {
        this.motifRejet = motifRejet;
    }

    public LocalDateTime getDateSoumission() {
        return dateSoumission;
    }

    public void setDateSoumission(LocalDateTime dateSoumission) {
        this.dateSoumission = dateSoumission;
    }

    public LocalDateTime getDateDecision() {
        return dateDecision;
    }

    public void setDateDecision(LocalDateTime dateDecision) {
        this.dateDecision = dateDecision;
    }

    public Long getValideParId() {
        return valideParId;
    }

    public void setValideParId(Long valideParId) {
        this.valideParId = valideParId;
    }

    public String getValideParNomComplet() {
        return valideParNomComplet;
    }

    public void setValideParNomComplet(String valideParNomComplet) {
        this.valideParNomComplet = valideParNomComplet;
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

    public BigDecimal getTotalHeuresSemaine() {
        return totalHeuresSemaine;
    }

    public void setTotalHeuresSemaine(BigDecimal totalHeuresSemaine) {
        this.totalHeuresSemaine = totalHeuresSemaine;
    }

    public BigDecimal getMontantEstime() {
        return montantEstime;
    }

    public void setMontantEstime(BigDecimal montantEstime) {
        this.montantEstime = montantEstime;
    }

    public Long getPvId() {
        return pvId;
    }

    public void setPvId(Long pvId) {
        this.pvId = pvId;
    }

    public String getNumeroPv() {
        return numeroPv;
    }

    public void setNumeroPv(String numeroPv) {
        this.numeroPv = numeroPv;
    }

    public List<LigneTimesheetDto> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneTimesheetDto> lignes) {
        this.lignes = lignes;
    }
}
