package com.fishing.brazil.service.captcha;

import com.fishing.brazil.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${google.recaptcha.secret}")
    private String recaptchaSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Falha fechado: qualquer erro — rede, resposta malformada, segredo ausente —
     * recusa a requisicao. Falhar aberto removeria silenciosamente a unica defesa
     * contra robos nos endpoints de conta, que e exatamente onde ela importa.
     */
    public void verificarFiltroAntiRobo(String recaptchaToken) {
        if (recaptchaToken == null || recaptchaToken.trim().isEmpty()) {
            throw new BusinessException("Token de segurança ausente. Atualize a página e tente novamente.");
        }

        String urlDoGoogle = "https://www.google.com/recaptcha/api/siteverify";

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("secret", recaptchaSecret);
        requestParams.add("response", recaptchaToken);

        Map<String, Object> apiResponse;
        try {
            apiResponse = restTemplate.postForObject(urlDoGoogle, requestParams, Map.class);
        } catch (RestClientException e) {
            // A mensagem do Google nao vai para o cliente: ela descreve a integracao,
            // e nao o que a pessoa pode fazer a respeito.
            throw new BusinessException("Não foi possível validar o selo de segurança. Tente novamente.");
        }

        if (apiResponse == null || !Boolean.TRUE.equals(apiResponse.get("success"))) {
            throw new BusinessException("Falha na verificação contra robôs (reCAPTCHA inválido).");
        }
    }
}
