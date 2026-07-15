package com.fishing.brazil.dto.request;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FishingSpotRequestDTO {
    private Long riverId;
    private String name;
    private Double latitude;
    private Double longitude;
    private String accessType;
    private String description;
}