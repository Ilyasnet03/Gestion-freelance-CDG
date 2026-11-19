package com.cdg.freelance.dto.timesheet;

import jakarta.validation.constraints.NotBlank;

public class RejectRequest {

    @NotBlank(message = "Le motif du rejet est obligatoire")
    private String motif;

    public RejectRequest() {}

    public RejectRequest(String motif) {
        this.motif = motif;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
