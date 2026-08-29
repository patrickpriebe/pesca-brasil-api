package com.fishing.brazil.config.web;

import com.fishing.brazil.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageableFactoryTest {

    private static final Set<String> ALLOWED = Set.of("name", "id");

    private Pageable build(int page, int size, String sortBy, String direction) {
        return PageableFactory.of(page, size, sortBy, direction, ALLOWED, "name", Sort.Direction.ASC);
    }

    @Test
    @DisplayName("uma propriedade fora da lista vira 400 nomeando o que existe")
    void propriedadeDesconhecidaEhRecusada() {
        assertThatThrownBy(() -> build(0, 10, "senha", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("senha")
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("a direção padrão é usada quando o cliente não informa")
    void direcaoPadrao() {
        Sort.Order order = build(0, 10, "name", null).getSort().getOrderFor("name");

        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("a direção informada prevalece, sem diferenciar maiúsculas")
    void direcaoInformada() {
        Sort.Order order = build(0, 10, "name", "desc").getSort().getOrderFor("name");

        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("uma direção inválida vira 400")
    void direcaoInvalida() {
        assertThatThrownBy(() -> build(0, 10, "name", "aleatoria"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ASC ou DESC");
    }

    @Test
    @DisplayName("size acima do teto vira 400 em vez de trazer a tabela inteira")
    void sizeAcimaDoTeto() {
        assertThatThrownBy(() -> build(0, 5000, "name", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100");
    }

    @Test
    @DisplayName("page negativo e size zero viram 400")
    void parametrosNegativos() {
        assertThatThrownBy(() -> build(-1, 10, "name", null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> build(0, 0, "name", null)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("sortBy vazio cai na propriedade padrão")
    void sortByVazioUsaOPadrao() {
        assertThat(build(0, 10, "  ", null).getSort().getOrderFor("name")).isNotNull();
    }
}
