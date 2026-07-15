package com.fishing.brazil.dto.request;
import com.fishing.brazil.enums.BaitType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaitRequestDTO {
    private String name;
    private BaitType type;
    private String description;
}