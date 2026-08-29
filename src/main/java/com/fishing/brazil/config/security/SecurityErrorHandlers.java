package com.fishing.brazil.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishing.brazil.exception.StandardError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * As recusas da cadeia de filtros tambem respondem no envelope da API.
 *
 * <p>Sem estes dois beans, o Spring Security devolve 403 com corpo vazio tanto para
 * quem nao mandou token quanto para quem mandou um token sem o papel necessario. Sao
 * situacoes diferentes — <em>entre primeiro</em> e <em>voce nao pode</em> — e o cliente
 * nao tem como distingui-las por um corpo que nao existe.
 */
@Configuration
public class SecurityErrorHandlers {

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> write(objectMapper, request, response,
                HttpStatus.UNAUTHORIZED, "Não Autorizado",
                "Autenticação necessária. Envie um token no header Authorization.");
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) -> write(objectMapper, request, response,
                HttpStatus.FORBIDDEN, "Acesso Negado",
                "Você não tem permissão para executar esta operação.");
    }

    private void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        StandardError body = new StandardError(
                LocalDateTime.now(), status.value(), error, message, request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }
}
