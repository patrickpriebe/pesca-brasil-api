package com.fishing.brazil.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishing.brazil.config.jwt.JwtAuthenticationFilter;
import com.fishing.brazil.config.security.SecurityConfig;
import com.fishing.brazil.config.security.SecurityErrorHandlers;
import com.fishing.brazil.dto.request.FishRequestDTO;
import com.fishing.brazil.dto.response.FishResponseDTO;
import com.fishing.brazil.service.FishService;
import com.fishing.brazil.service.login.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * As regras de autorizacao do catalogo, exercidas pela cadeia de filtros real.
 *
 * <p>A distincao que importa aqui e entre autenticado e administrador: escrever no
 * catalogo muda o que todo mundo ve, e ate a versao anterior qualquer conta verificada
 * podia faze-lo.
 */
@WebMvcTest(FishController.class)
@Import({SecurityConfig.class, SecurityErrorHandlers.class, JwtAuthenticationFilter.class})
class FishControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FishService fishService;

    // O filtro real entra na cadeia; o que e mockado sao as suas dependencias.
    // Um @MockBean do proprio filtro nao chama doFilter, e toda requisicao
    // morre no meio da cadeia devolvendo 200 com corpo vazio.
    @MockBean
    private com.fishing.brazil.config.jwt.JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private FishRequestDTO validPayload() {
        FishRequestDTO dto = new FishRequestDTO();
        dto.setCommonName("Dourado");
        dto.setScientificName("Salminus brasiliensis");
        return dto;
    }

    @Test
    @DisplayName("a listagem é pública")
    void listagemEhPublica() throws Exception {
        Page<FishResponseDTO> page = new PageImpl<>(List.of());
        when(fishService.findFishes(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/fishes"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cadastrar sem token responde 401")
    void cadastroAnonimoEhRecusado() throws Exception {
        mockMvc.perform(post("/api/fishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isUnauthorized());

        verify(fishService, never()).save(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_PESCADOR")
    @DisplayName("um pescador autenticado não escreve no catálogo")
    void pescadorNaoEscreveNoCatalogo() throws Exception {
        mockMvc.perform(post("/api/fishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isForbidden());

        verify(fishService, never()).save(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("um administrador escreve no catálogo")
    void administradorEscreveNoCatalogo() throws Exception {
        when(fishService.save(any())).thenReturn(new FishResponseDTO());

        mockMvc.perform(post("/api/fishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "ROLE_PESCADOR")
    @DisplayName("um pescador autenticado não remove do catálogo")
    void pescadorNaoRemoveDoCatalogo() throws Exception {
        mockMvc.perform(delete("/api/fishes/1"))
                .andExpect(status().isForbidden());

        verify(fishService, never()).delete(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("um payload sem nome científico responde 400 com o campo apontado")
    void validacaoApontaOCampo() throws Exception {
        FishRequestDTO invalido = new FishRequestDTO();
        invalido.setCommonName("Dourado");

        mockMvc.perform(post("/api/fishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.scientificName").exists());
    }

    @Test
    @DisplayName("ordenar por uma propriedade inexistente responde 400, e não 500")
    void ordenacaoInvalidaEh400() throws Exception {
        mockMvc.perform(get("/api/fishes").param("sortBy", "senha"))
                .andExpect(status().isBadRequest());
    }
}
