package com.fishing.brazil.service.captcha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${google.recaptcha.secret}")
    private String recaptchaSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public void verificarFiltroAntiRobo(String recaptchaToken) {
        if (recaptchaToken == null || recaptchaToken.trim().isEmpty()) {
            throw new RuntimeException("Token de segurança ausente. Atualize a página e tente novamente.");
        }

        String urlDoGoogle = "https://www.google.com/recaptcha/api/siteverify";

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("secret", recaptchaSecret);
        requestParams.add("response", recaptchaToken);

        try {
            Map<String, Object> apiResponse = restTemplate.postForObject(
                    urlDoGoogle,
                    requestParams,
                    Map.class
            );

            if (apiResponse == null || !Boolean.TRUE.equals(apiResponse.get("success"))) {
                throw new RuntimeException("Falha na verificação contra robôs (reCAPTCHA inválido).");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao validar o selo de segurança: " + e.getMessage());
        }
    }
}