package com.cdg.freelance.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "freelance")
public class Freelance implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tjm;

    @Column(length = 100)
    private String specialite;

    @Column(nullable = false, length = 30)
    private String statut = "ACTIF";

    public Freelance() {}

    public Freelance(Utilisateur utilisateur, BigDecimal tjm, String specialite, String statut) {
        this.utilisateur = utilisateur;
        this.tjm = tjm;
        this.specialite = specialite;
        this.statut = statut != null ? statut : "ACTIF";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public BigDecimal getTjm() {
        return tjm;
    }

    public void setTjm(BigDecimal tjm) {
        this.tjm = tjm;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
