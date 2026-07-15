package com.fishing.brazil.service.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String name, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bem-vindo ao Pesca Brasil - Código de Verificação");
        message.setText("Olá, " + name + "!\n\n"
                + "Obrigado por se registrar no Pesca Brasil. Para ativar sua conta, "
                + "use o código de segurança de 6 dígitos abaixo:\n\n"
                + "CÓDIGO: " + code + "\n\n"
                + "Este código expira em 15 minutos.\n\n"
                + "Boas pescarias,\nEquipe Pesca Brasil.");

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String name, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Pesca Brasil - Recuperação de Senha");
        message.setText("Olá, " + name + "!\n\n"
                + "Recebemos um pedido para redefinir a sua senha. "
                + "Use o código de segurança abaixo para criar uma nova senha:\n\n"
                + "CÓDIGO: " + code + "\n\n"
                + "Este código expira em 15 minutos. Se você não solicitou isso, "
                + "apenas ignore este e-mail. A sua conta está segura.\n\n"
                + "Boas pescarias,\nEquipe Pesca Brasil.");

        mailSender.send(message);
    }
}