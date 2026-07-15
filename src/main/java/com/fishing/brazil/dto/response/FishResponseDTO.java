package com.fishing.brazil.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FishResponseDTO {

    private Long id;
    private String commonName;
    private String scientificName;
    private String conservationStatus;
    private String description;
    private String imageUrl;
}