package com.cdg.freelance.dto.facturation;

import com.cdg.freelance.entity.PaiementStatut;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PaiementDto {

    private Long id;
    private Long pvId;
    private String numeroPv;
    private Long timesheetId;
    private String freelanceNomComplet;
    private String projetNom;
    private BigDecimal montant;
    private PaiementStatut statut;
    private LocalDate datePaiement;
    private String reference;

    public PaiementDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getProjetNom() {
        return projetNom;
    }

    public void setProjetNom(String projetNom) {
        this.projetNom = projetNom;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public PaiementStatut getStatut() {
        return statut;
    }

    public void setStatut(PaiementStatut statut) {
        this.statut = statut;
    }

    public LocalDate getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(LocalDate datePaiement) {
        this.datePaiement = datePaiement;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
