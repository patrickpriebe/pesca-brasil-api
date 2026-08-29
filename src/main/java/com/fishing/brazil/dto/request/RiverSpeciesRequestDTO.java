package com.fishing.brazil.dto.request;

import com.fishing.brazil.enums.Abundance;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiverSpeciesRequestDTO {

    @NotNull(message = "O rio é obrigatório.")
    private Long riverId;

    @NotNull(message = "O peixe é obrigatório.")
    private Long fishId;

    @NotNull(message = "A abundância é obrigatória (ALTA, MEDIA, BAIXA ou RARA).")
    private Abundance abundance;

    private String bestSeason;
}
