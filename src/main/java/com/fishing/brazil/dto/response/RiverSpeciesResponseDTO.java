package com.fishing.brazil.dto.response;
import com.fishing.brazil.enums.Abundance;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiverSpeciesResponseDTO {
    private Long id;
    private Long riverId;
    private String riverName;
    private Long fishId;
    private String fishCommonName;
    private Abundance abundance;
    private String bestSeason;
}