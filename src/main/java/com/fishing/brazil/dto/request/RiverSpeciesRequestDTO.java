package com.fishing.brazil.dto.request;
import com.fishing.brazil.enums.Abundance;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiverSpeciesRequestDTO {
    private Long riverId;
    private Long fishId;
    private Abundance abundance;
    private String bestSeason;
}