package com.cdg.freelance.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "projet")
public class Projet implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "heures_prevues", precision = 10, scale = 2)
    private BigDecimal heuresPrevues;

    @Column(name = "nombre_taches_prevues")
    private Integer nombreTachesPrevues;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin_prevue", nullable = false)
    private LocalDate dateFinPrevue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjetStatut statut = ProjetStatut.PLANIFIE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_projet_id", nullable = false)
    private TypeProjet typeProjet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chef_projet_id", nullable = false)
    private Utilisateur chefProjet;

    public Projet() {}

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

    public BigDecimal getHeuresPrevues() { return heuresPrevues; }
    public void setHeuresPrevues(BigDecimal heuresPrevues) { this.heuresPrevues = heuresPrevues; }
    public Integer getNombreTachesPrevues() { return nombreTachesPrevues; }
    public void setNombreTachesPrevues(Integer nombreTachesPrevues) { this.nombreTachesPrevues = nombreTachesPrevues; }

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

    public TypeProjet getTypeProjet() {
        return typeProjet;
    }

    public void setTypeProjet(TypeProjet typeProjet) {
        this.typeProjet = typeProjet;
    }

    public Utilisateur getChefProjet() {
        return chefProjet;
    }

    public void setChefProjet(Utilisateur chefProjet) {
        this.chefProjet = chefProjet;
    }
}
