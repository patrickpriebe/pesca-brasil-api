package com.fishing.brazil.service.login;

import com.fishing.brazil.entity.login.Role;
import com.fishing.brazil.entity.login.User;
import com.fishing.brazil.enums.login.RoleName;
import com.fishing.brazil.repository.login.RoleRepository;
import com.fishing.brazil.repository.login.UserRepository;
import com.fishing.brazil.service.email.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class UserService {

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

    public User registerUser(String name, String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Este e-mail já está em uso!");
        }

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(rawPassword));

        newUser.setEnabled(false);

        String otp = String.format("%06d", new Random().nextInt(999999));
        newUser.setVerificationCode(otp);
        newUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15)); // Expira em 15 min

        Role userRole = roleRepository.findByName(RoleName.ROLE_PESCADOR)
                .orElseThrow(() -> new RuntimeException("Perfil de acesso não encontrado."));
        newUser.getRoles().add(userRole);

        User savedUser = userRepository.save(newUser);

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getName(), otp);

        return savedUser;
    }

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

    public void generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Não encontramos uma conta com este e-mail."));

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setVerificationCode(otp);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), otp);
    }

    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (user.getVerificationCodeExpiresAt() == null || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("O código de segurança expirou. Solicite um novo.");
        }

        if (!user.getVerificationCode().equals(code)) {
            throw new RuntimeException("Código de segurança inválido.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        userRepository.save(user);
    }
}