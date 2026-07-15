package com.fishing.brazil.dto.response;
import com.fishing.brazil.enums.BaitType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaitResponseDTO {
    private Long id;
    private String name;
    private BaitType type;
    private String description;
}