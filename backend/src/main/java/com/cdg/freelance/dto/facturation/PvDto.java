package com.cdg.freelance.dto.facturation;

import com.cdg.freelance.entity.PaiementStatut;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PvDto {

    private Long id;
    private Long timesheetId;
    private String freelanceNomComplet;
    private String freelanceSpecialite;
    private String projetCode;
    private String projetNom;
    private LocalDate semaineDebut;
    private String numero;
    private BigDecimal totalHeures;
    private BigDecimal montant;
    private String cheminPdf;
    private LocalDateTime dateGeneration;
    private PaiementStatut paiementStatut;
    private Long paiementId;

    public PvDto() {}

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

    public String getProjetCode() {
        return projetCode;
    }

    public void setProjetCode(String projetCode) {
        this.projetCode = projetCode;
    }

    public String getProjetNom() {
        return projetNom;
    }

    public void setProjetNom(String projetNom) {
        this.projetNom = projetNom;
    }

    public LocalDate getSemaineDebut() {
        return semaineDebut;
    }

    public void setSemaineDebut(LocalDate semaineDebut) {
        this.semaineDebut = semaineDebut;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public BigDecimal getTotalHeures() {
        return totalHeures;
    }

    public void setTotalHeures(BigDecimal totalHeures) {
        this.totalHeures = totalHeures;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getCheminPdf() {
        return cheminPdf;
    }

    public void setCheminPdf(String cheminPdf) {
        this.cheminPdf = cheminPdf;
    }

    public LocalDateTime getDateGeneration() {
        return dateGeneration;
    }

    public void setDateGeneration(LocalDateTime dateGeneration) {
        this.dateGeneration = dateGeneration;
    }

    public PaiementStatut getPaiementStatut() {
        return paiementStatut;
    }

    public void setPaiementStatut(PaiementStatut paiementStatut) {
        this.paiementStatut = paiementStatut;
    }

    public Long getPaiementId() {
        return paiementId;
    }

    public void setPaiementId(Long paiementId) {
        this.paiementId = paiementId;
    }
}
