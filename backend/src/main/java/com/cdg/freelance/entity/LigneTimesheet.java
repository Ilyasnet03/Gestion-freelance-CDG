package com.cdg.freelance.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ligne_timesheet", uniqueConstraints = {
    @UniqueConstraint(name = "uq_ligne", columnNames = {"timesheet_id", "tache_id", "date_saisie"})
})
public class LigneTimesheet implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false)
    private Timesheet timesheet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tache_id", nullable = false)
    private Tache tache;

    @Column(name = "date_saisie", nullable = false)
    private LocalDate dateSaisie;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal heures;

    public LigneTimesheet() {}

    public LigneTimesheet(Timesheet timesheet, Tache tache, LocalDate dateSaisie, BigDecimal heures) {
        this.timesheet = timesheet;
        this.tache = tache;
        this.dateSaisie = dateSaisie;
        this.heures = heures;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timesheet getTimesheet() {
        return timesheet;
    }

    public void setTimesheet(Timesheet timesheet) {
        this.timesheet = timesheet;
    }

    public Tache getTache() {
        return tache;
    }

    public void setTache(Tache tache) {
        this.tache = tache;
    }

    public LocalDate getDateSaisie() {
        return dateSaisie;
    }

    public void setDateSaisie(LocalDate dateSaisie) {
        this.dateSaisie = dateSaisie;
    }

    public BigDecimal getHeures() {
        return heures;
    }

    public void setHeures(BigDecimal heures) {
        this.heures = heures;
    }
}
