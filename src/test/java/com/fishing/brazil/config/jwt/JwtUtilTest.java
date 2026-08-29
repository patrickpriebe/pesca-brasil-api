package com.fishing.brazil.config.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O contrato do token.
 *
 * <p>Este e o teste que vale mais do que qualquer outro nesta suite: e o unico lugar
 * onde uma regressao silenciosa seria um desvio de autenticacao, e nao uma tela
 * quebrada. Um token assinado com outra chave precisa ser recusado, e um token
 * expirado precisa ser recusado como expirado.
 */
class JwtUtilTest {

    private static final String SECRET = "chave-de-teste-com-mais-de-32-caracteres-para-hs256";
    private static final String OTHER_SECRET = "outra-chave-de-teste-com-mais-de-32-caracteres-aqui";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
    }

    private UserDetails userDetails(String email) {
        return new User(email, "irrelevante", List.of());
    }

    @Test
    @DisplayName("o token gerado carrega o e-mail como subject")
    void tokenCarregaOSubject() {
        String token = jwtUtil.generateToken("pescador@example.com");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("pescador@example.com");
    }

    @Test
    @DisplayName("o token vale para o usuário que o recebeu")
    void tokenValidoParaODono() {
        String token = jwtUtil.generateToken("pescador@example.com");

        assertThat(jwtUtil.isTokenValid(token, userDetails("pescador@example.com"))).isTrue();
    }

    @Test
    @DisplayName("um token não vale para outro usuário")
    void tokenNaoValeParaOutroUsuario() {
        String token = jwtUtil.generateToken("pescador@example.com");

        assertThat(jwtUtil.isTokenValid(token, userDetails("outro@example.com"))).isFalse();
    }

    @Test
    @DisplayName("um token assinado com outra chave é recusado")
    void tokenDeOutraChaveEhRecusado() {
        JwtUtil impostor = new JwtUtil();
        ReflectionTestUtils.setField(impostor, "secretKey", OTHER_SECRET);

        String tokenForjado = impostor.generateToken("pescador@example.com");

        // A recusa e uma excecao ao verificar a assinatura, e nao um false silencioso:
        // um token com assinatura invalida nao chega a ter claims em que confiar.
        assertThatThrownBy(() -> jwtUtil.extractUsername(tokenForjado))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    @DisplayName("um token adulterado é recusado")
    void tokenAdulteradoEhRecusado() {
        String token = jwtUtil.generateToken("pescador@example.com");
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtUtil.extractUsername(adulterado))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("a aplicação recusa subir com um segredo curto demais para HS256")
    void segredoCurtoImpedeAInicializacao() {
        JwtUtil comSegredoCurto = new JwtUtil();
        ReflectionTestUtils.setField(comSegredoCurto, "secretKey", "curto-demais");

        assertThatThrownBy(comSegredoCurto::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("a aplicação recusa subir sem segredo nenhum")
    void segredoAusenteImpedeAInicializacao() {
        JwtUtil semSegredo = new JwtUtil();
        ReflectionTestUtils.setField(semSegredo, "secretKey", null);

        assertThatThrownBy(semSegredo::validateSecret)
                .isInstanceOf(IllegalStateException.class);
    }
}
