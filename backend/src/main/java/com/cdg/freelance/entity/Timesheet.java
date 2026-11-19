package com.cdg.freelance.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timesheet", uniqueConstraints = {
    @UniqueConstraint(name = "uq_timesheet_semaine", columnNames = {"freelance_id", "semaine_debut"})
})
public class Timesheet implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "freelance_id", nullable = false)
    private Freelance freelance;

    @Column(name = "semaine_debut", nullable = false)
    private LocalDate semaineDebut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimesheetStatut statut = TimesheetStatut.BROUILLON;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "valide_par")
    private Utilisateur validePar;

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LigneTimesheet> lignes = new ArrayList<>();

    public Timesheet() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Freelance getFreelance() {
        return freelance;
    }

    public void setFreelance(Freelance freelance) {
        this.freelance = freelance;
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

    public Utilisateur getValidePar() {
        return validePar;
    }

    public void setValidePar(Utilisateur validePar) {
        this.validePar = validePar;
    }

    public List<LigneTimesheet> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneTimesheet> lignes) {
        this.lignes = lignes;
    }
}
