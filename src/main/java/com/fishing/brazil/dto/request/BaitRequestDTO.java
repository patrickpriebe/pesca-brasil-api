package com.fishing.brazil.dto.request;

import com.fishing.brazil.enums.BaitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaitRequestDTO {

    @NotBlank(message = "O nome da isca é obrigatório.")
    private String name;

    @NotNull(message = "O tipo da isca é obrigatório (ARTIFICIAL ou NATURAL).")
    private BaitType type;

    private String description;
}
