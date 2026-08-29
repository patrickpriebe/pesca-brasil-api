package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.CatchRecordRequestDTO;
import com.fishing.brazil.entity.CatchRecord;
import com.fishing.brazil.entity.Fish;
import com.fishing.brazil.entity.FishingSpot;
import com.fishing.brazil.entity.River;
import com.fishing.brazil.entity.login.User;
import com.fishing.brazil.enums.login.RoleName;
import com.fishing.brazil.exception.NotFoundException;
import com.fishing.brazil.repository.*;
import com.fishing.brazil.repository.login.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatchRecordServiceTest {

    @Mock private CatchRecordRepository catchRecordRepository;
    @Mock private FishRepository fishRepository;
    @Mock private FishingSpotRepository fishingSpotRepository;
    @Mock private BaitRepository baitRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private RiverRepository riverRepository;

    private CatchRecordService service;

    private User dono;
    private River rio;

    @BeforeEach
    void setUp() {
        service = new CatchRecordService(catchRecordRepository, fishRepository, fishingSpotRepository,
                baitRepository, equipmentRepository, userRepository, riverRepository);

        dono = new User();
        dono.setId(1L);
        dono.setName("Patrick");
        dono.setEmail("dono@example.com");

        rio = new River();
        rio.setId(7L);
        rio.setName("Rio Paraná");

        Fish peixe = new Fish();
        peixe.setId(3L);
        peixe.setCommonName("Dourado");

        when(userRepository.findByEmail("dono@example.com")).thenReturn(Optional.of(dono));
        when(fishRepository.findById(3L)).thenReturn(Optional.of(peixe));
        when(riverRepository.findById(7L)).thenReturn(Optional.of(rio));
        when(catchRecordRepository.save(any(CatchRecord.class))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, String... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, authorities));
    }

    private CatchRecordRequestDTO comCoordenada() {
        CatchRecordRequestDTO dto = new CatchRecordRequestDTO();
        dto.setFishId(3L);
        dto.setRiverId(7L);
        dto.setLatitude(-25.5163);
        dto.setLongitude(-54.5854);
        dto.setCatchDate(LocalDateTime.now());
        return dto;
    }

    @Test
    @DisplayName("sem um ponto próximo, a coordenada cria um ponto de pesca novo")
    void coordenadaCriaPontoNovo() {
        authenticateAs("dono@example.com", RoleName.ROLE_PESCADOR.name());
        when(fishingSpotRepository.findNearby(anyLong(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        when(fishingSpotRepository.save(any(FishingSpot.class))).thenAnswer(call -> {
            FishingSpot spot = call.getArgument(0);
            spot.setId(99L);
            return spot;
        });

        service.save(comCoordenada());

        verify(fishingSpotRepository).save(argThat(spot ->
                spot.getRiver().equals(rio)
                        && spot.getName().equals("Ponto no Rio Paraná")
                        && spot.getAccessType().equals("Não especificado")));
    }

    @Test
    @DisplayName("com um ponto próximo, a coordenada reaproveita o ponto existente")
    void coordenadaProximaReaproveitaOPonto() {
        authenticateAs("dono@example.com", RoleName.ROLE_PESCADOR.name());

        FishingSpot existente = new FishingSpot();
        existente.setId(42L);
        existente.setRiver(rio);
        existente.setName("Remanso da ponte");

        when(fishingSpotRepository.findNearby(anyLong(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(existente));

        service.save(comCoordenada());

        // A tabela cresce uma vez por lugar, e nao uma vez por peixe.
        verify(fishingSpotRepository, never()).save(any(FishingSpot.class));
    }

    @Test
    @DisplayName("o nome enviado prevalece sobre o nome derivado do rio")
    void nomeDoPontoVemDoCliente() {
        authenticateAs("dono@example.com", RoleName.ROLE_PESCADOR.name());
        when(fishingSpotRepository.findNearby(anyLong(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        when(fishingSpotRepository.save(any(FishingSpot.class))).thenAnswer(call -> call.getArgument(0));

        CatchRecordRequestDTO dto = comCoordenada();
        dto.setSpotName("  Remanso da ponte  ");

        service.save(dto);

        verify(fishingSpotRepository).save(argThat(spot -> spot.getName().equals("Remanso da ponte")));
    }

    @Test
    @DisplayName("o dono vem do token, nunca do corpo da requisição")
    void donoVemDoToken() {
        authenticateAs("dono@example.com", RoleName.ROLE_PESCADOR.name());
        when(fishingSpotRepository.findNearby(anyLong(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        when(fishingSpotRepository.save(any(FishingSpot.class))).thenAnswer(call -> call.getArgument(0));

        service.save(comCoordenada());

        verify(catchRecordRepository).save(argThat(record -> record.getUser().equals(dono)));
    }

    @Test
    @DisplayName("um peixe inexistente é recusado")
    void peixeInexistenteEhRecusado() {
        authenticateAs("dono@example.com", RoleName.ROLE_PESCADOR.name());
        when(fishRepository.findById(999L)).thenReturn(Optional.empty());

        CatchRecordRequestDTO dto = comCoordenada();
        dto.setFishId(999L);

        assertThatThrownBy(() -> service.save(dto)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("com fishingSpotId, a coordenada é ignorada e o ponto é resolvido")
    void fishingSpotIdIgnoraACoordenada() {
        authenticateAs("dono@example.com", RoleName.ROLE_PESCADOR.name());

        FishingSpot curado = new FishingSpot();
        curado.setId(5L);
        curado.setRiver(rio);
        curado.setName("Ponto curado");
        when(fishingSpotRepository.findById(5L)).thenReturn(Optional.of(curado));

        CatchRecordRequestDTO dto = comCoordenada();
        dto.setFishingSpotId(5L);

        service.save(dto);

        verify(fishingSpotRepository, never()).save(any(FishingSpot.class));
        verify(fishingSpotRepository, never()).findNearby(anyLong(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("o dono remove o próprio registro")
    void donoRemoveOProprioRegistro() {
        authenticateAs("dono@example.com", RoleName.ROLE_PESCADOR.name());

        CatchRecord record = new CatchRecord();
        record.setId(10L);
        record.setUser(dono);
        when(catchRecordRepository.findById(10L)).thenReturn(Optional.of(record));

        service.delete(10L);

        verify(catchRecordRepository).delete(record);
    }

    @Test
    @DisplayName("o registro de outra pessoa responde igual a um inexistente")
    void registroDeOutraPessoaRespondeComoInexistente() {
        authenticateAs("intruso@example.com", RoleName.ROLE_PESCADOR.name());

        CatchRecord record = new CatchRecord();
        record.setId(10L);
        record.setUser(dono);
        when(catchRecordRepository.findById(10L)).thenReturn(Optional.of(record));
        when(catchRecordRepository.findById(404L)).thenReturn(Optional.empty());

        String mensagemDeOutro = capturarMensagem(10L);
        String mensagemInexistente = capturarMensagem(404L);

        assertThat(mensagemDeOutro).isEqualTo(mensagemInexistente);
        verify(catchRecordRepository, never()).delete(any());
    }

    private String capturarMensagem(Long id) {
        try {
            service.delete(id);
            throw new AssertionError("esperava NotFoundException para o id " + id);
        } catch (NotFoundException e) {
            return e.getMessage();
        }
    }

    @Test
    @DisplayName("um administrador remove o registro de qualquer pessoa")
    void administradorRemoveQualquerRegistro() {
        authenticateAs("admin@example.com", RoleName.ROLE_ADMIN.name());

        CatchRecord record = new CatchRecord();
        record.setId(10L);
        record.setUser(dono);
        when(catchRecordRepository.findById(10L)).thenReturn(Optional.of(record));

        service.delete(10L);

        verify(catchRecordRepository).delete(record);
    }
}
