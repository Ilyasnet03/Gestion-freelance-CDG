package com.cdg.freelance.dto.timesheet;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateTimesheetRequest {

    @NotNull(message = "La date de début de semaine est obligatoire")
    private LocalDate semaineDebut;

    public CreateTimesheetRequest() {}

    public CreateTimesheetRequest(LocalDate semaineDebut) {
        this.semaineDebut = semaineDebut;
    }

    public LocalDate getSemaineDebut() {
        return semaineDebut;
    }

    public void setSemaineDebut(LocalDate semaineDebut) {
        this.semaineDebut = semaineDebut;
    }
}
