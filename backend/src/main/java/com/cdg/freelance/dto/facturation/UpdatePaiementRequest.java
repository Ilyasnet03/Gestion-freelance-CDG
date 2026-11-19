package com.cdg.freelance.dto.facturation;

import com.cdg.freelance.entity.PaiementStatut;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class UpdatePaiementRequest {

    @NotNull(message = "Le statut est obligatoire")
    private PaiementStatut statut;

    private LocalDate datePaiement;
    private String reference;

    public UpdatePaiementRequest() {}

    public UpdatePaiementRequest(PaiementStatut statut, LocalDate datePaiement, String reference) {
        this.statut = statut;
        this.datePaiement = datePaiement;
        this.reference = reference;
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
