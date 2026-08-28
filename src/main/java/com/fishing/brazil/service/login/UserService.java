package com.fishing.brazil.service.login;

import com.fishing.brazil.entity.login.Role;
import com.fishing.brazil.entity.login.User;
import com.fishing.brazil.enums.login.RoleName;
import com.fishing.brazil.repository.login.RoleRepository;
import com.fishing.brazil.repository.login.UserRepository;
import com.fishing.brazil.service.email.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class UserService {

    // SecureRandom, e nao Random: o codigo de seis digitos e uma credencial de curta
    // duracao, e um PRNG previsivel deixa de proteger a conta assim que a sequencia
    // for observada algumas vezes.
    private static final SecureRandom OTP_GENERATOR = new SecureRandom();
    private static final int OTP_UPPER_BOUND = 1_000_000;
    private static final int OTP_VALIDITY_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public User registerUser(String name, String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Este e-mail já está em uso!");
        }

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(rawPassword));

        newUser.setEnabled(false);

        String otp = generateOtp();
        newUser.setVerificationCode(otp);
        newUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));

        Role userRole = roleRepository.findByName(RoleName.ROLE_PESCADOR)
                .orElseThrow(() -> new RuntimeException("Perfil de acesso não encontrado."));
        newUser.getRoles().add(userRole);

        User savedUser = userRepository.save(newUser);

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getName(), otp);

        return savedUser;
    }

    @Transactional
    public void verifyAccount(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (user.isEnabled()) {
            throw new RuntimeException("Esta conta já está verificada.");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("O código de verificação expirou. Solicite um novo.");
        }

        if (!user.getVerificationCode().equals(code)) {
            throw new RuntimeException("Código de verificação inválido.");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
    }

    /**
     * Nao sinaliza se a conta existe. Responder "nao encontramos uma conta com este
     * e-mail" transforma o endpoint em um verificador de cadastro que qualquer um pode
     * percorrer; quem realmente tem a conta recebe o codigo, e quem nao tem recebe a
     * mesma resposta do controller.
     */
    @Transactional
    public void generatePasswordResetToken(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String otp = generateOtp();
            user.setVerificationCode(otp);
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));

            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), otp);
        });
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // Uma unica mensagem para e-mail inexistente, codigo errado e codigo expirado:
        // mensagens distintas responderiam se a conta existe, que e exatamente o que
        // generatePasswordResetToken deixou de responder.
        String genericFailure = "Código de segurança inválido ou expirado. Solicite um novo.";

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(genericFailure));

        if (user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(genericFailure);
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new RuntimeException(genericFailure);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        userRepository.save(user);
    }

    private String generateOtp() {
        return String.format("%06d", OTP_GENERATOR.nextInt(OTP_UPPER_BOUND));
    }
}