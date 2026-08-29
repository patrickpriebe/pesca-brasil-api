package com.fishing.brazil.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FishResponseDTO {
    private Long id;
    private String commonName;
    private String scientificName;
    private String conservationStatus;
    private String description;
    private String imageUrl;

    // As recomendacoes eram graváveis por POST /api/fishes e invisiveis na leitura:
    // a relacao existia no banco e nao saia pela API. Sao as duas listas que
    // transformam um catalogo em conselho — que isca usar para qual especie.
    private List<BaitResponseDTO> recommendedBaits = new ArrayList<>();
    private List<EquipmentResponseDTO> recommendedEquipments = new ArrayList<>();
}
