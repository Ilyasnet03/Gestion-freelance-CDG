package com.cdg.freelance.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "parametre")
public class Parametre implements Serializable {

    @Id
    @Column(length = 100)
    private String cle;

    @Column(nullable = false, length = 255)
    private String valeur;

    public Parametre() {}

    public Parametre(String cle, String valeur) {
        this.cle = cle;
        this.valeur = valeur;
    }

    public String getCle() {
        return cle;
    }

    public void setCle(String cle) {
        this.cle = cle;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }
}
