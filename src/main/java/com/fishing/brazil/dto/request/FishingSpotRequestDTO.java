package com.fishing.brazil.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FishingSpotRequestDTO {

    @NotNull(message = "O rio é obrigatório.")
    private Long riverId;

    @NotBlank(message = "O nome do ponto é obrigatório.")
    private String name;

    @NotNull(message = "A latitude é obrigatória.")
    @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90.")
    @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90.")
    private Double latitude;

    @NotNull(message = "A longitude é obrigatória.")
    @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180.")
    @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180.")
    private Double longitude;

    private String accessType;
    private String description;
}
