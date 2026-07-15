package com.fishing.brazil.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class FishRequestDTO {

    @NotBlank(message = "O nome comum do peixe é obrigatório.")
    private String commonName;

    @NotBlank(message = "O nome científico é obrigatório.")
    private String scientificName;

    private String conservationStatus;
    private String description;
    private String imageUrl;

    private List<Long> recommendedBaitIds;
    private List<Long> recommendedEquipmentIds;
}