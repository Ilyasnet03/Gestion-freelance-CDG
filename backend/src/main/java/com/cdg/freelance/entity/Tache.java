package com.cdg.freelance.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tache")
public class Tache implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "affectation_id")
    private Affectation affectation;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(name = "charge_estimee", nullable = false, precision = 6, scale = 1)
    private BigDecimal chargeEstimee = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer avancement = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TacheStatut statut = TacheStatut.A_FAIRE;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    public Tache() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Projet getProjet() {
        return projet;
    }

    public void setProjet(Projet projet) {
        this.projet = projet;
    }

    public Affectation getAffectation() {
        return affectation;
    }

    public void setAffectation(Affectation affectation) {
        this.affectation = affectation;
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
