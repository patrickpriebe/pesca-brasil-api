package com.fishing.brazil.dto.request;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class FishingRegulationRequestDTO {
    private String hydrographicBasin;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}