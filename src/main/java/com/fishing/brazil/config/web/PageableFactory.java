package com.fishing.brazil.config.web;

import com.fishing.brazil.exception.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Monta o Pageable a partir dos parametros de consulta, validando o que vem do cliente.
 *
 * <p>Antes, {@code sortBy} ia direto para {@code Sort.by(...)}: uma propriedade
 * inexistente virava uma falha em tempo de execucao no meio da consulta, em vez de um
 * 400 dizendo qual campo nao existe. A lista de propriedades permitidas por recurso e
 * declarada no controller, que e quem sabe o que aquele recurso expoe.
 *
 * <p>A direcao tambem passou a ser um parametro. Antes cada endpoint fixava a sua, o
 * que tornava impossivel, por exemplo, listar as capturas mais antigas primeiro.
 */
public final class PageableFactory {

    private static final int MAX_PAGE_SIZE = 100;

    private PageableFactory() {
    }

    public static Pageable of(int page, int size, String sortBy, String direction,
                              Set<String> allowedProperties, String defaultProperty,
                              Sort.Direction defaultDirection) {

        if (page < 0) {
            throw new BusinessException("O parâmetro page não pode ser negativo.");
        }
        if (size < 1) {
            throw new BusinessException("O parâmetro size precisa ser maior que zero.");
        }
        // Um teto para o size impede que uma unica requisicao peca a tabela inteira e
        // segure a conexao enquanto monta a resposta.
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException("O parâmetro size não pode ser maior que " + MAX_PAGE_SIZE + ".");
        }

        String property = (sortBy == null || sortBy.trim().isEmpty()) ? defaultProperty : sortBy.trim();

        if (!allowedProperties.contains(property)) {
            throw new BusinessException("Não é possível ordenar por \"" + property
                    + "\". Campos disponíveis: " + String.join(", ", allowedProperties.stream().sorted().toList()) + ".");
        }

        Sort.Direction resolvedDirection = defaultDirection;
        if (direction != null && !direction.trim().isEmpty()) {
            String normalized = direction.trim().toUpperCase();
            if (!normalized.equals("ASC") && !normalized.equals("DESC")) {
                throw new BusinessException("O parâmetro direction aceita apenas ASC ou DESC.");
            }
            resolvedDirection = Sort.Direction.valueOf(normalized);
        }

        return PageRequest.of(page, size, Sort.by(resolvedDirection, property));
    }
}
