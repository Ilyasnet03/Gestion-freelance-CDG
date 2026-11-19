package com.cdg.freelance.dto.projet;

import com.cdg.freelance.entity.ProjetStatut;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProjetDto {

    private Long id;
    private String code;
    private String nom;
    private String description;
    private BigDecimal budget;
    private LocalDate dateDebut;
    private LocalDate dateFinPrevue;
    private ProjetStatut statut;
    private Long typeProjetId;
    private String typeProjetLibelle;
    private Long chefProjetId;
    private String chefProjetNomComplet;

    // Aggregates for dashboard & list display
    private BigDecimal budgetConsomme;
    private Double tauxConsommation;
    private Integer avancementGlobal;

    public ProjetDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
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

    public ProjetStatut getStatut() {
        return statut;
    }

    public void setStatut(ProjetStatut statut) {
        this.statut = statut;
    }

    public Long getTypeProjetId() {
        return typeProjetId;
    }

    public void setTypeProjetId(Long typeProjetId) {
        this.typeProjetId = typeProjetId;
    }

    public String getTypeProjetLibelle() {
        return typeProjetLibelle;
    }

    public void setTypeProjetLibelle(String typeProjetLibelle) {
        this.typeProjetLibelle = typeProjetLibelle;
    }

    public Long getChefProjetId() {
        return chefProjetId;
    }

    public void setChefProjetId(Long chefProjetId) {
        this.chefProjetId = chefProjetId;
    }

    public String getChefProjetNomComplet() {
        return chefProjetNomComplet;
    }

    public void setChefProjetNomComplet(String chefProjetNomComplet) {
        this.chefProjetNomComplet = chefProjetNomComplet;
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

    public Integer getAvancementGlobal() {
        return avancementGlobal;
    }

    public void setAvancementGlobal(Integer avancementGlobal) {
        this.avancementGlobal = avancementGlobal;
    }
}
