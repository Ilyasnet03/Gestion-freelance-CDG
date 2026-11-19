package com.cdg.freelance.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "paiement")
public class Paiement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pv_id", nullable = false)
    private PvReception pvReception;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaiementStatut statut = PaiementStatut.EN_ATTENTE;

    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    @Column(length = 100)
    private String reference;

    public Paiement() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PvReception getPvReception() {
        return pvReception;
    }

    public void setPvReception(PvReception pvReception) {
        this.pvReception = pvReception;
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
