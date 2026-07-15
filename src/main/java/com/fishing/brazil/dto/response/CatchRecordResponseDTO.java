package com.fishing.brazil.dto.response;

import com.fishing.brazil.enums.CatchOutcome;
import com.fishing.brazil.enums.MoonPhase;
import com.fishing.brazil.enums.WeatherCondition;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CatchRecordResponseDTO {
    private Long id;
    private String userName;

    private Long fishId;
    private String fishName;

    private Long fishingSpotId;
    private String fishingSpotName;

    private Long baitId;
    private String baitName;

    private Long equipmentId;

    private Double weightInKg;
    private Double lengthInCm;
    private LocalDateTime catchDate;

    private WeatherCondition weatherCondition;
    private MoonPhase moonPhase;
    private CatchOutcome outcome;

    private String photoUrl;
    private String notes;
}