package com.fishing.brazil.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cada excecao de dominio tem o seu proprio status.
 *
 * <p>A versao anterior mapeava toda RuntimeException para 404, porque o caso comum era
 * um id inexistente. O efeito colateral era que uma falha real do servidor tambem era
 * reportada como "nao encontrado", e o cliente nao tinha como distinguir as duas.
 * Agora o que nao for reconhecido e 500, que e o que de fato e.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<StandardError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Não Encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Requisição Inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<StandardError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<StandardError> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Muitas Requisições", ex.getMessage(), request);
    }

    /**
     * Credenciais erradas e conta desativada continuam respondendo 400, e nao 401.
     * O interceptor do frontend desloga em 401 e 403, e uma falha de login e
     * justamente onde a mensagem precisa permanecer na tela. A mensagem original do
     * Spring ("Bad credentials", "User is disabled") e preservada porque o cliente
     * decide o texto exibido a partir dela.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<StandardError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Falha na Autenticação", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Acesso Negado",
                "Você não tem permissão para executar esta operação.", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<StandardError> handleUploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo Muito Grande",
                "A imagem enviada excede o limite de 5 MB.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.merge(fieldError.getField(), fieldError.getDefaultMessage(),
                    (existing, incoming) -> existing + ", " + incoming);
        }

        String summary = String.join(", ", fieldErrors.values());

        StandardError error = new StandardError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de Validação",
                summary,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Qualquer coisa nao prevista e 500 e vai para o log com o stack trace. A mensagem
     * devolvida e generica de proposito: o texto de uma excecao interna costuma
     * descrever a estrutura do sistema para quem estiver sondando.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro nao tratado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno",
                "Ocorreu um erro inesperado. Tente novamente em instantes.", request);
    }

    private ResponseEntity<StandardError> build(HttpStatus status, String error, String message,
                                                HttpServletRequest request) {
        StandardError body = new StandardError(
                LocalDateTime.now(), status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
