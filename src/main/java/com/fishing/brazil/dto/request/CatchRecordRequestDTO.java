package com.fishing.brazil.dto.request;

import com.fishing.brazil.enums.CatchOutcome;
import com.fishing.brazil.enums.MoonPhase;
import com.fishing.brazil.enums.WeatherCondition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CatchRecordRequestDTO {

    @NotNull(message = "O peixe é obrigatório.")
    private Long fishId;

    @NotNull(message = "A data e hora da captura são obrigatórias.")
    private LocalDateTime catchDate;

    // Ponto de pesca: ou o id de um ponto ja registrado, ou o rio mais a coordenada
    // marcada no mapa. Nenhum dos dois e obrigatorio isoladamente, porque exigir os
    // tres campos do mapa mesmo quando o id foi enviado obrigava o cliente a preencher
    // dados que a API descarta.
    private Long fishingSpotId;

    private Long riverId;
    private Double latitude;
    private Double longitude;
    private String spotName;

    private Long baitId;
    private Long equipmentId;

    @Positive(message = "O peso deve ser maior que zero.")
    private Double weightInKg;

    @Positive(message = "O comprimento deve ser maior que zero.")
    private Double lengthInCm;

    private WeatherCondition weatherCondition;
    private MoonPhase moonPhase;
    private CatchOutcome outcome;

    private String photoUrl;
    private String notes;

    /**
     * Exatamente um dos dois caminhos precisa estar completo. A mensagem aparece sob
     * "localizacao" porque e o campo que o formulario mostra, e nao sob um dos tres
     * campos que compoem a alternativa.
     */
    @JsonIgnore
    @AssertTrue(message = "Informe um ponto de pesca já cadastrado ou marque o ponto no mapa.")
    public boolean isLocalizacaoInformada() {
        if (fishingSpotId != null) {
            return true;
        }
        return riverId != null && latitude != null && longitude != null;
    }

    /** Coordenadas fora destes limites nao sao um ponto no planeta. */
    @JsonIgnore
    @AssertTrue(message = "Coordenada inválida: latitude entre -90 e 90, longitude entre -180 e 180.")
    public boolean isCoordenadaValida() {
        if (latitude == null && longitude == null) {
            return true;
        }
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }
}
