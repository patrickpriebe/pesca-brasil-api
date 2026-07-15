package com.fishing.brazil.dto.response;
import com.fishing.brazil.enums.EquipmentType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentResponseDTO {
    private Long id;
    private EquipmentType type;
    private String recommendedLineWeight;
    private String action;
}