package com.fishing.brazil.config.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishing.brazil.exception.StandardError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Janela fixa por endereco de origem sobre /api/auth/**.
 *
 * <p>Estes sao os endpoints cujo custo nao e nosso: cada registro dispara um e-mail,
 * cada login e reset consulta o reCAPTCHA, e as cotas gratuitas por tras disso — SMTP,
 * banco — sao o que quebra primeiro sob automacao.
 *
 * <p><strong>Depende de {@code server.forward-headers-strategy=framework}.</strong> Atras
 * do proxy do Render, {@code getRemoteAddr()} devolveria o endereco do proxy para todo
 * mundo, e o limite passaria a valer para a base inteira como se fosse um unico cliente.
 * Com a estrategia ligada, o Spring reescreve a requisicao a partir de
 * {@code X-Forwarded-For} antes de qualquer filtro nosso rodar.
 *
 * <p>O estado vive em memoria, entao ele se perde a cada reinicio e nao e compartilhado
 * entre instancias. Para uma instancia unica isso e suficiente; a partir de duas, o
 * limite efetivo vira o dobro do configurado, e a solucao passa a ser a borda.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PREFIX = "/api/auth/";

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pescabrasil.rate-limit.requests-per-window:20}")
    private int maxRequests;

    @Value("${pescabrasil.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Value("${pescabrasil.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith(PROTECTED_PREFIX);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String client = request.getRemoteAddr();
        Instant now = Instant.now();

        Window window = windows.compute(client, (key, existing) -> {
            if (existing == null || existing.hasExpired(now, windowSeconds)) {
                return new Window(now);
            }
            return existing;
        });

        if (window.count.incrementAndGet() > maxRequests) {
            writeTooManyRequests(request, response);
            return;
        }

        // Sem esta limpeza o mapa cresce com um registro por endereco visto desde o
        // ultimo reinicio, que e um vazamento lento com cara de uso normal de memoria.
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().hasExpired(now, windowSeconds));
        }

        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(windowSeconds));

        StandardError error = new StandardError(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Muitas Requisições",
                "Muitas tentativas em pouco tempo. Aguarde um minuto e tente novamente.",
                request.getRequestURI());

        objectMapper.findAndRegisterModules();
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private static final class Window {
        private final Instant startedAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(Instant startedAt) {
            this.startedAt = startedAt;
        }

        private boolean hasExpired(Instant now, long windowSeconds) {
            return Duration.between(startedAt, now).getSeconds() >= windowSeconds;
        }
    }
}
