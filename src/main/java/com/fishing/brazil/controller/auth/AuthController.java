package com.fishing.brazil.controller.auth;

import com.fishing.brazil.config.jwt.JwtUtil;
import com.fishing.brazil.dto.request.login.*;
import com.fishing.brazil.dto.response.auth.AuthResponseDTO;
import com.fishing.brazil.entity.login.User;
import com.fishing.brazil.repository.login.UserRepository;
import com.fishing.brazil.service.captcha.RecaptchaService;
import com.fishing.brazil.service.login.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO request) {
        try {
            recaptchaService.verificarFiltroAntiRobo(request.getRecaptchaToken());

            User newUser = userService.registerUser(request.getName(), request.getEmail(), request.getPassword());
            return ResponseEntity.ok("Usuário registrado com sucesso! ID: " + newUser.getId());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        try {
            recaptchaService.verificarFiltroAntiRobo(request.getRecaptchaToken());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String jwt = jwtUtil.generateToken(userDetails.getUsername());

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            String role = user.getRoles().iterator().next().getName().name();

            return ResponseEntity.ok(new AuthResponseDTO(jwt, user.getName(), user.getEmail(), role));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestBody VerifyRequestDTO request) {
        try {
            userService.verifyAccount(request.getEmail(), request.getCode());
            return ResponseEntity.ok("Conta ativada com sucesso! Você já pode fazer login.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        try {
            userService.generatePasswordResetToken(request.getEmail());
            return ResponseEntity.ok("Código de recuperação enviado para o seu e-mail.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        try {
            userService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok("Senha alterada com sucesso! Você já pode fazer login.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}