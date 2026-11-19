package com.cdg.freelance.dto.auth;

import com.cdg.freelance.entity.Role;

public class UserDto {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private Role role;
    private Boolean actif;
    private Long freelanceId; // If role is FREELANCE

    public UserDto() {}

    public UserDto(Long id, String nom, String prenom, String email, Role role, Boolean actif, Long freelanceId) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
        this.actif = actif;
        this.freelanceId = freelanceId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Long getFreelanceId() {
        return freelanceId;
    }

    public void setFreelanceId(Long freelanceId) {
        this.freelanceId = freelanceId;
    }

    public String getNomComplet() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }
}
