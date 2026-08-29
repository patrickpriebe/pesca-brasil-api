package com.fishing.brazil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@code @EnableAsync} e o que faz o {@code @Async} do EmailService valer. Sem ele a
 * anotacao e inerte e o envio roda na thread da requisicao, dentro da transacao que
 * acabou de inserir o usuario — colocando um timeout de SMTP dentro dela.
 */
@EnableAsync
@SpringBootApplication
public class BrazilApplication {

	public static void main(String[] args) {
		SpringApplication.run(BrazilApplication.class, args);
	}

}
