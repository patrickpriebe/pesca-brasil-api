package com.fishing.brazil.dto.response.ranking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingPescadorResponseDTO {
    private int posicao;
    private String pescador;
    private Long capturas;
    private Long diasNaAgua;
}