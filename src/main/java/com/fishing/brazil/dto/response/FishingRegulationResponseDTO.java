package com.fishing.brazil.dto.response;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class FishingRegulationResponseDTO {
    private Long id;
    private String hydrographicBasin;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}