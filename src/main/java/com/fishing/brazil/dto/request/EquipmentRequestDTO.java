package com.fishing.brazil.dto.request;

import com.fishing.brazil.enums.EquipmentType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentRequestDTO {

    @NotNull(message = "O tipo do equipamento é obrigatório (MOLINETE ou CARRETILHA).")
    private EquipmentType type;

    private String recommendedLineWeight;
    private String action;
}
