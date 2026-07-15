package com.fishing.brazil.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiverRequestDTO {
    private String name;
    private String hydrographicBasin;
    private String description;
}