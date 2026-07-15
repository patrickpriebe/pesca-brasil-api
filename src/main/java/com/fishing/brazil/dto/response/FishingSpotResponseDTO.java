package com.fishing.brazil.dto.response;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FishingSpotResponseDTO {
    private Long id;
    private Long riverId;
    private String riverName;
    private String name;
    private Double latitude;
    private Double longitude;
    private String accessType;
    private String description;
}