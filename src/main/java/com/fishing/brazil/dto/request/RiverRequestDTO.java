package com.fishing.brazil.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiverRequestDTO {

    @NotBlank(message = "O nome do rio é obrigatório.")
    private String name;

    @NotBlank(message = "A bacia hidrográfica é obrigatória.")
    private String hydrographicBasin;

    private String description;
}
