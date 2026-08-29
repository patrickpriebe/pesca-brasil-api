package com.fishing.brazil.controller.auth;

import com.fishing.brazil.config.jwt.JwtUtil;
import com.fishing.brazil.dto.request.login.*;
import com.fishing.brazil.dto.response.auth.AuthResponseDTO;
import com.fishing.brazil.entity.login.User;
import com.fishing.brazil.exception.NotFoundException;
import com.fishing.brazil.repository.login.UserRepository;
import com.fishing.brazil.service.captcha.RecaptchaService;
import com.fishing.brazil.service.login.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Nenhum handler aqui captura excecoes: o GlobalExceptionHandler traduz cada tipo de
 * dominio no seu proprio status. A versao anterior capturava RuntimeException em todo
 * metodo e devolvia a mensagem como texto puro, o que colapsava conflito, validacao e
 * falha interna num unico 400.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RecaptchaService recaptchaService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserService userService,
                          UserRepository userRepository,
                          RecaptchaService recaptchaService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.userRepository = userRepository;
        this.recaptchaService = recaptchaService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequestDTO request) {
        recaptchaService.verificarFiltroAntiRobo(request.getRecaptchaToken());

        User newUser = userService.registerUser(request.getName(), request.getEmail(), request.getPassword());

        return ResponseEntity.ok(Map.of(
                "message", "Usuário registrado com sucesso! Verifique o seu e-mail.",
                "id", String.valueOf(newUser.getId())));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        recaptchaService.verificarFiltroAntiRobo(request.getRecaptchaToken());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String jwt = jwtUtil.generateToken(userDetails.getUsername());

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        String role = user.getRoles().stream()
                .map(r -> r.getName().name())
                .sorted()
                .findFirst()
                .orElse("ROLE_PESCADOR");

        return ResponseEntity.ok(new AuthResponseDTO(jwt, user.getName(), user.getEmail(), role));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyAccount(@Valid @RequestBody VerifyRequestDTO request) {
        userService.verifyAccount(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "Conta ativada com sucesso! Você já pode fazer login."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        recaptchaService.verificarFiltroAntiRobo(request.getRecaptchaToken());

        userService.generatePasswordResetToken(request.getEmail());

        // Sempre a mesma resposta, exista a conta ou nao: o unico sinal de que o
        // e-mail esta cadastrado deve ser a mensagem que chega na caixa de entrada.
        return ResponseEntity.ok(Map.of(
                "message", "Se existir uma conta com este e-mail, enviamos um código de recuperação."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        userService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso! Você já pode fazer login."));
    }
}
