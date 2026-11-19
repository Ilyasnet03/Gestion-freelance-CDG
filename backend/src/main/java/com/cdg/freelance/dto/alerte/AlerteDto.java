package com.cdg.freelance.dto.alerte;

import com.cdg.freelance.entity.AlerteNiveau;
import com.cdg.freelance.entity.AlerteType;
import java.time.LocalDateTime;

public class AlerteDto {

    private Long id;
    private Long projetId;
    private String projetCode;
    private String projetNom;
    private AlerteType type;
    private AlerteNiveau niveau;
    private String message;
    private LocalDateTime dateCreation;
    private Boolean traitee;

    public AlerteDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjetId() {
        return projetId;
    }

    public void setProjetId(Long projetId) {
        this.projetId = projetId;
    }

    public String getProjetCode() {
        return projetCode;
    }

    public void setProjetCode(String projetCode) {
        this.projetCode = projetCode;
    }

    public String getProjetNom() {
        return projetNom;
    }

    public void setProjetNom(String projetNom) {
        this.projetNom = projetNom;
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
