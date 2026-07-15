package com.fishing.brazil.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiverResponseDTO {
    private Long id;
    private String name;
    private String hydrographicBasin;
    private String description;
}