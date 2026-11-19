package com.cdg.freelance.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class ParametreDto {

    @NotBlank(message = "La clé est obligatoire")
    private String cle;

    @NotBlank(message = "La valeur est obligatoire")
    private String valeur;

    public ParametreDto() {}

    public ParametreDto(String cle, String valeur) {
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
