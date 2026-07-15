package com.fishing.brazil.dto.response.ranking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingPeixeResponseDTO {
    private int posicao;
    private String pescador;
    private String especie;
    private String local;
    private Double medida;
    private String fotoUrl;
}