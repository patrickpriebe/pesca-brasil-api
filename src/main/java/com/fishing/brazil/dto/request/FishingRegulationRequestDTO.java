package com.fishing.brazil.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FishingRegulationRequestDTO {

    @NotBlank(message = "A bacia hidrográfica é obrigatória.")
    private String hydrographicBasin;

    @NotNull(message = "A data de início do defeso é obrigatória.")
    private LocalDate startDate;

    @NotNull(message = "A data de término do defeso é obrigatória.")
    private LocalDate endDate;

    private String notes;

    /**
     * Um defeso que termina antes de comecar nunca fecha, e a consulta por bacia o
     * devolveria como um periodo valido.
     */
    @JsonIgnore
    @AssertTrue(message = "A data de término do defeso deve ser posterior à data de início.")
    public boolean isPeriodoValido() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
