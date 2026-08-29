package com.fishing.brazil.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * O esquema de seguranca e o que faz o botao <em>Authorize</em> do Swagger UI
     * funcionar: sem ele o explorador monta requisicoes sem o header Authorization e
     * todo endpoint protegido responde 401, o que parece defeito da API.
     */
    @Bean
    public OpenAPI pescaBrasilOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pesca Brasil API")
                        .version("v1")
                        .description("""
                                Diário de pesca esportiva brasileira.

                                Toda requisição GET sob /api é pública. As demais exigem um token \
                                Bearer, e as escritas de catálogo exigem também ROLE_ADMIN — que \
                                nenhum endpoint concede: a promoção é feita no banco.""")
                        .contact(new Contact()
                                .name("Patrick Priebe")
                                .url("https://github.com/patrickpriebe"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devolvido por POST /api/auth/login.")));
    }
}
