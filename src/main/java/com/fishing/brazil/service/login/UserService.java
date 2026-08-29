package com.fishing.brazil.service.login;

import com.fishing.brazil.entity.login.Role;
import com.fishing.brazil.entity.login.User;
import com.fishing.brazil.enums.login.RoleName;
import com.fishing.brazil.exception.BusinessException;
import com.fishing.brazil.exception.ConflictException;
import com.fishing.brazil.exception.NotFoundException;
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

    /**
     * Um codigo de seis digitos tem um milhao de valores e quinze minutos de vida.
     * Sem um teto de tentativas, esse espaco e percorrivel por forca bruta dentro da
     * janela; o contador invalida o codigo antes que isso valha a pena.
     */
    private static final int MAX_CODE_ATTEMPTS = 5;

    private static final int MINIMUM_PASSWORD_LENGTH = 8;

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
        validatePassword(rawPassword);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Este e-mail já está em uso!");
        }

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(rawPassword));

        newUser.setEnabled(false);

        issueCode(newUser);

        Role userRole = roleRepository.findByName(RoleName.ROLE_PESCADOR)
                .orElseThrow(() -> new NotFoundException("Perfil de acesso não encontrado."));
        newUser.getRoles().add(userRole);

        User savedUser = userRepository.save(newUser);

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getName(), newUser.getVerificationCode());

        return savedUser;
    }

    @Transactional
    public void verifyAccount(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        if (user.isEnabled()) {
            throw new ConflictException("Esta conta já está verificada.");
        }

        // A expiracao e testada antes da correcao de proposito: um codigo vencido e
        // recusado como vencido tenha ele acertado ou nao, e a alternativa avisaria a
        // quem tem um codigo antigo que ele ao menos era valido.
        if (user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("O código de verificação expirou. Solicite um novo.");
        }

        if (!codeMatches(user, code)) {
            userRepository.save(user);
            throw new BusinessException("Código de verificação inválido.");
        }

        user.setEnabled(true);
        clearCode(user);
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
            issueCode(user);
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), user.getVerificationCode());
        });
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        validatePassword(newPassword);

        // Uma unica mensagem para e-mail inexistente, codigo errado e codigo expirado:
        // mensagens distintas responderiam se a conta existe, que e exatamente o que
        // generatePasswordResetToken deixou de responder.
        String genericFailure = "Código de segurança inválido ou expirado. Solicite um novo.";

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(genericFailure));

        if (user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(genericFailure);
        }

        if (!codeMatches(user, code)) {
            userRepository.save(user);
            throw new BusinessException(genericFailure);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        clearCode(user);

        userRepository.save(user);
    }

    /**
     * Comprimento minimo e a parte util de uma politica de senha; regras de composicao
     * em geral produzem senhas piores, porque empurram todo mundo para o mesmo padrao.
     */
    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new BusinessException(
                    "A senha precisa ter no mínimo " + MINIMUM_PASSWORD_LENGTH + " caracteres.");
        }
    }

    private void issueCode(User user) {
        user.setVerificationCode(generateOtp());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        user.setVerificationAttempts(0);
    }

    private void clearCode(User user) {
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        user.setVerificationAttempts(0);
    }

    /**
     * Conta a tentativa antes de responder. Estourado o teto, o codigo e destruido —
     * o proximo palpite nao tem mais nada para acertar, ainda que seja o certo.
     */
    private boolean codeMatches(User user, String code) {
        if (user.getVerificationCode() != null && user.getVerificationCode().equals(code)) {
            return true;
        }

        user.setVerificationAttempts(currentAttempts(user) + 1);
        if (currentAttempts(user) >= MAX_CODE_ATTEMPTS) {
            clearCode(user);
        }
        return false;
    }

    // O contador e anulavel no banco: linhas criadas antes desta coluna existirem
    // leem null, e null aqui significa nenhuma tentativa registrada.
    private int currentAttempts(User user) {
        return user.getVerificationAttempts() == null ? 0 : user.getVerificationAttempts();
    }

    private String generateOtp() {
        return String.format("%06d", OTP_GENERATOR.nextInt(OTP_UPPER_BOUND));
    }
}
