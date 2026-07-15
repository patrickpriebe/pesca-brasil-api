package com.fishing.brazil.dto.request;
import com.fishing.brazil.enums.EquipmentType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentRequestDTO {
    private EquipmentType type;
    private String recommendedLineWeight;
    private String action;
}