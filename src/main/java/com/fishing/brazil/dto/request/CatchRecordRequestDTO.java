package com.fishing.brazil.dto.request;

import com.fishing.brazil.enums.CatchOutcome;
import com.fishing.brazil.enums.MoonPhase;
import com.fishing.brazil.enums.WeatherCondition;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CatchRecordRequestDTO {

    @NotNull(message = "O peixe é obrigatório.")
    private Long fishId;

    private Long fishingSpotId;

    private Long baitId;
    private Long equipmentId;

    private Double weightInKg;
    private Double lengthInCm;

    @NotNull(message = "A data e hora da captura são obrigatórias.")
    private LocalDateTime catchDate;

    private WeatherCondition weatherCondition;
    private MoonPhase moonPhase;
    private CatchOutcome outcome;

    private String photoUrl;
    private String notes;

    @NotNull(message = "O rio é obrigatório.")
    private Long riverId;

    @NotNull(message = "Marque o ponto no mapa.")
    private Double latitude;

    @NotNull(message = "Marque o ponto no mapa.")
    private Double longitude;

    private String spotName;
}