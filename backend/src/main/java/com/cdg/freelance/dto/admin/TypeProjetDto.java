package com.cdg.freelance.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class TypeProjetDto {

    private Long id;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    public TypeProjetDto() {}

    public TypeProjetDto(Long id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}
