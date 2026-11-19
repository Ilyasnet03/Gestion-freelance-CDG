package com.cdg.freelance.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerte", indexes = {
    @Index(name = "idx_alerte_projet_traitee", columnList = "projet_id, traitee")
})
public class Alerte implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlerteType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlerteNiveau niveau;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean traitee = false;

    public Alerte() {}

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

    public AlerteType getType() {
        return type;
    }

    public void setType(AlerteType type) {
        this.type = type;
    }

    public AlerteNiveau getNiveau() {
        return niveau;
    }

    public void setNiveau(AlerteNiveau niveau) {
        this.niveau = niveau;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Boolean getTraitee() {
        return traitee;
    }

    public void setTraitee(Boolean traitee) {
        this.traitee = traitee;
    }
}
