package com.cdg.freelance.dto.projet;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProjetAvancementDto {

    private Long projetId;
    private String code;
    private String nom;
    private BigDecimal budget;
    private BigDecimal budgetConsomme;
    private Double tauxConsommation; // en %
    private Integer avancementReel; // en %
    private Double avancementAttendu; // en %
    private Double ecartAvancement; // en points
    private LocalDate dateDebut;
    private LocalDate dateFinPrevue;
    private Long joursEcoules;
    private Long dureePrevueJours;

    public ProjetAvancementDto() {}

    public Long getProjetId() {
        return projetId;
    }

    public void setProjetId(Long projetId) {
        this.projetId = projetId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public BigDecimal getBudgetConsomme() {
        return budgetConsomme;
    }

    public void setBudgetConsomme(BigDecimal budgetConsomme) {
        this.budgetConsomme = budgetConsomme;
    }

    public Double getTauxConsommation() {
        return tauxConsommation;
    }

    public void setTauxConsommation(Double tauxConsommation) {
        this.tauxConsommation = tauxConsommation;
    }

    public Integer getAvancementReel() {
        return avancementReel;
    }

    public void setAvancementReel(Integer avancementReel) {
        this.avancementReel = avancementReel;
    }

    public Double getAvancementAttendu() {
        return avancementAttendu;
    }

    public void setAvancementAttendu(Double avancementAttendu) {
        this.avancementAttendu = avancementAttendu;
    }

    public Double getEcartAvancement() {
        return ecartAvancement;
    }

    public void setEcartAvancement(Double ecartAvancement) {
        this.ecartAvancement = ecartAvancement;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFinPrevue() {
        return dateFinPrevue;
    }

    public void setDateFinPrevue(LocalDate dateFinPrevue) {
        this.dateFinPrevue = dateFinPrevue;
    }

    public Long getJoursEcoules() {
        return joursEcoules;
    }

    public void setJoursEcoules(Long joursEcoules) {
        this.joursEcoules = joursEcoules;
    }

    public Long getDureePrevueJours() {
        return dureePrevueJours;
    }

    public void setDureePrevueJours(Long dureePrevueJours) {
        this.dureePrevueJours = dureePrevueJours;
    }
}
