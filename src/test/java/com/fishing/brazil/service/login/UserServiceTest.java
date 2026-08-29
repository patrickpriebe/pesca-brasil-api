package com.fishing.brazil.service.login;

import com.fishing.brazil.entity.login.Role;
import com.fishing.brazil.entity.login.User;
import com.fishing.brazil.enums.login.RoleName;
import com.fishing.brazil.exception.BusinessException;
import com.fishing.brazil.exception.ConflictException;
import com.fishing.brazil.repository.login.RoleRepository;
import com.fishing.brazil.repository.login.UserRepository;
import com.fishing.brazil.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailService emailService;

    @SuppressWarnings("unused")
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder, emailService);
    }

    private User existingUser(String code, LocalDateTime expiresAt) {
        User user = new User();
        user.setId(1L);
        user.setName("Patrick");
        user.setEmail("pescador@example.com");
        user.setPassword("hash");
        user.setEnabled(false);
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(expiresAt);
        return user;
    }

    @Nested
    @DisplayName("registro")
    class Registro {

        @Test
        @DisplayName("recusa uma senha abaixo do mínimo antes de tocar no banco")
        void senhaCurtaEhRecusada() {
            assertThatThrownBy(() -> userService.registerUser("Patrick", "novo@example.com", "1234"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("8 caracteres");

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("recusa um e-mail já cadastrado com 409, e não 404")
        void emailDuplicadoEhConflito() {
            when(userRepository.findByEmail("pescador@example.com"))
                    .thenReturn(Optional.of(existingUser(null, null)));

            assertThatThrownBy(() ->
                    userService.registerUser("Patrick", "pescador@example.com", "senha-valida-123"))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("cria a conta desabilitada, com senha em hash e um código de seis dígitos")
        void contaNasceDesabilitada() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(roleRepository.findByName(RoleName.ROLE_PESCADOR))
                    .thenReturn(Optional.of(new Role(1L, RoleName.ROLE_PESCADOR)));
            when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

            userService.registerUser("Patrick", "novo@example.com", "senha-valida-123");

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());

            User user = saved.getValue();
            assertThat(user.isEnabled()).isFalse();
            assertThat(user.getPassword()).isNotEqualTo("senha-valida-123").startsWith("$2");
            assertThat(user.getVerificationCode()).matches("[0-9]{6}");
            assertThat(user.getVerificationCodeExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(user.getRoles()).extracting(Role::getName).containsExactly(RoleName.ROLE_PESCADOR);
        }
    }

    @Nested
    @DisplayName("verificação de conta")
    class Verificacao {

        @Test
        @DisplayName("um código expirado é recusado como expirado, mesmo estando correto")
        void expiracaoEhTestadaAntesDaCorrecao() {
            User user = existingUser("123456", LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByEmail("pescador@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.verifyAccount("pescador@example.com", "123456"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expirou");
        }

        @Test
        @DisplayName("o código é destruído ao ser usado, para que não possa ser repetido")
        void codigoEhDestruidoNoUso() {
            User user = existingUser("123456", LocalDateTime.now().plusMinutes(10));
            when(userRepository.findByEmail("pescador@example.com")).thenReturn(Optional.of(user));

            userService.verifyAccount("pescador@example.com", "123456");

            assertThat(user.isEnabled()).isTrue();
            assertThat(user.getVerificationCode()).isNull();
            assertThat(user.getVerificationCodeExpiresAt()).isNull();
        }

        @Test
        @DisplayName("uma conta já verificada responde 409")
        void contaJaVerificada() {
            User user = existingUser("123456", LocalDateTime.now().plusMinutes(10));
            user.setEnabled(true);
            when(userRepository.findByEmail("pescador@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.verifyAccount("pescador@example.com", "123456"))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("cinco tentativas erradas destroem o código, e a sexta não acerta mais")
        void tetoDeTentativasInvalidaOCodigo() {
            User user = existingUser("123456", LocalDateTime.now().plusMinutes(10));
            when(userRepository.findByEmail("pescador@example.com")).thenReturn(Optional.of(user));

            for (int tentativa = 1; tentativa <= 5; tentativa++) {
                assertThatThrownBy(() -> userService.verifyAccount("pescador@example.com", "000000"))
                        .isInstanceOf(BusinessException.class);
            }

            assertThat(user.getVerificationCode()).isNull();

            // O codigo certo tambem deixa de valer: nao ha mais nada para acertar.
            assertThatThrownBy(() -> userService.verifyAccount("pescador@example.com", "123456"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("recuperação de senha")
    class Recuperacao {

        @Test
        @DisplayName("um e-mail não cadastrado não gera erro nem e-mail")
        void emailInexistenteNaoRevelaNada() {
            when(userRepository.findByEmail("ninguem@example.com")).thenReturn(Optional.empty());

            userService.generatePasswordResetToken("ninguem@example.com");

            verify(userRepository, never()).save(any());
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("um e-mail cadastrado recebe um código novo")
        void emailCadastradoRecebeCodigo() {
            User user = existingUser(null, null);
            when(userRepository.findByEmail("pescador@example.com")).thenReturn(Optional.of(user));

            userService.generatePasswordResetToken("pescador@example.com");

            assertThat(user.getVerificationCode()).matches("[0-9]{6}");
            verify(emailService).sendPasswordResetEmail(eq("pescador@example.com"), eq("Patrick"), anyString());
        }

        @Test
        @DisplayName("e-mail desconhecido, código errado e código expirado dão a mesma mensagem")
        void falhasSaoIndistinguiveis() {
            when(userRepository.findByEmail("ninguem@example.com")).thenReturn(Optional.empty());

            User comCodigoErrado = existingUser("123456", LocalDateTime.now().plusMinutes(10));
            when(userRepository.findByEmail("pescador@example.com")).thenReturn(Optional.of(comCodigoErrado));

            User expirado = existingUser("123456", LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByEmail("expirado@example.com")).thenReturn(Optional.of(expirado));

            String mensagemDesconhecido = capturarMensagem("ninguem@example.com", "123456");
            String mensagemCodigoErrado = capturarMensagem("pescador@example.com", "999999");
            String mensagemExpirado = capturarMensagem("expirado@example.com", "123456");

            assertThat(mensagemDesconhecido)
                    .isEqualTo(mensagemCodigoErrado)
                    .isEqualTo(mensagemExpirado);
        }

        private String capturarMensagem(String email, String codigo) {
            try {
                userService.resetPassword(email, codigo, "senha-valida-123");
                throw new AssertionError("esperava uma falha para " + email);
            } catch (BusinessException e) {
                return e.getMessage();
            }
        }

        @Test
        @DisplayName("a nova senha também passa pela política de comprimento")
        void novaSenhaCurtaEhRecusada() {
            assertThatThrownBy(() ->
                    userService.resetPassword("pescador@example.com", "123456", "curta"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("8 caracteres");
        }
    }

    @Test
    @DisplayName("o código gerado usa todo o intervalo de seis dígitos, inclusive 999999")
    void codigoCobreOIntervaloInteiro() {
        // nextInt(999999) nunca produzia 999999. O teste nao prova a distribuicao, mas
        // fixa o formato: seis digitos, sempre, com zeros a esquerda preservados.
        UserService service = new UserService(userRepository, roleRepository, passwordEncoder, emailService);

        for (int i = 0; i < 200; i++) {
            String otp = (String) ReflectionTestUtils.invokeMethod(service, "generateOtp");
            assertThat(otp).matches("[0-9]{6}");
        }
    }
}
