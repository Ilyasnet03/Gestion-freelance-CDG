package com.cdg.freelance.dto.tache;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateAvancementRequest {

    @NotNull(message = "Le pourcentage d'avancement est obligatoire")
    @Min(value = 0, message = "L'avancement minimum est 0")
    @Max(value = 100, message = "L'avancement maximum est 100")
    private Integer avancement;

    public UpdateAvancementRequest() {}

    public UpdateAvancementRequest(Integer avancement) {
        this.avancement = avancement;
    }

    public Integer getAvancement() {
        return avancement;
    }

    public void setAvancement(Integer avancement) {
        this.avancement = avancement;
    }
}
