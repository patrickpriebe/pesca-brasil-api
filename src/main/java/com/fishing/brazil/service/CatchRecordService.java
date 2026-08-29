package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.CatchRecordRequestDTO;
import com.fishing.brazil.dto.response.CatchRecordResponseDTO;
import com.fishing.brazil.dto.response.ranking.RankingPeixeResponseDTO;
import com.fishing.brazil.dto.response.ranking.RankingPescadorResponseDTO;
import com.fishing.brazil.entity.CatchRecord;
import com.fishing.brazil.entity.FishingSpot;
import com.fishing.brazil.entity.River;
import com.fishing.brazil.entity.login.User;
import com.fishing.brazil.enums.login.RoleName;
import com.fishing.brazil.exception.BusinessException;
import com.fishing.brazil.exception.NotFoundException;
import com.fishing.brazil.repository.*;
import com.fishing.brazil.repository.login.UserRepository;
import com.fishing.brazil.repository.projection.RankingPescadorProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatchRecordService {

    /**
     * Meia janela, em graus, usada para reconhecer que uma coordenada nova e o mesmo
     * lugar de um ponto que ja existe. 0.0005 grau de latitude e da ordem de 55 metros,
     * que e a escala em que duas marcacoes do mesmo pesqueiro se confundem.
     *
     * <p>Em longitude o mesmo valor cobre menos distancia conforme se afasta do equador.
     * No Brasil o encolhimento chega a cerca de 15%, e essa diferenca nao muda qual
     * ponto e escolhido.
     */
    private static final double SAME_SPOT_DEGREES = 0.0005;

    private static final int RANKING_SIZE = 10;

    private final CatchRecordRepository catchRecordRepository;
    private final FishRepository fishRepository;
    private final FishingSpotRepository fishingSpotRepository;
    private final BaitRepository baitRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final RiverRepository riverRepository;

    public CatchRecordService(
            CatchRecordRepository catchRecordRepository,
            FishRepository fishRepository,
            FishingSpotRepository fishingSpotRepository,
            BaitRepository baitRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            RiverRepository riverRepository) {
        this.catchRecordRepository = catchRecordRepository;
        this.fishRepository = fishRepository;
        this.fishingSpotRepository = fishingSpotRepository;
        this.baitRepository = baitRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.riverRepository = riverRepository;
    }

    public Page<CatchRecordResponseDTO> findAll(String search, Pageable pageable) {
        Page<CatchRecord> page = (search == null || search.trim().isEmpty())
                ? catchRecordRepository.findAll(pageable)
                : catchRecordRepository.buscarComFiltro(search.trim(), pageable);

        return page.map(this::convertToResponseDTO);
    }

    /** O diario de quem esta autenticado, sem depender de um id vindo do cliente. */
    public Page<CatchRecordResponseDTO> findMine(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return catchRecordRepository.findByUserEmail(email, pageable).map(this::convertToResponseDTO);
    }

    @Transactional
    public CatchRecordResponseDTO save(CatchRecordRequestDTO dto) {
        String emailDoPescador = SecurityContextHolder.getContext().getAuthentication().getName();
        User pescadorLogado = userRepository.findByEmail(emailDoPescador)
                .orElseThrow(() -> new NotFoundException("Usuário logado não encontrado no banco."));

        CatchRecord record = new CatchRecord();
        record.setUser(pescadorLogado);

        record.setFish(fishRepository.findById(dto.getFishId())
                .orElseThrow(() -> new NotFoundException("Peixe não encontrado.")));

        record.setFishingSpot(resolveFishingSpot(dto));

        if (dto.getBaitId() != null) {
            record.setBait(baitRepository.findById(dto.getBaitId())
                    .orElseThrow(() -> new NotFoundException("Isca não encontrada.")));
        }

        if (dto.getEquipmentId() != null) {
            record.setEquipment(equipmentRepository.findById(dto.getEquipmentId())
                    .orElseThrow(() -> new NotFoundException("Equipamento não encontrado.")));
        }

        record.setWeightInKg(dto.getWeightInKg());
        record.setLengthInCm(dto.getLengthInCm());
        record.setCatchDate(dto.getCatchDate());
        record.setWeatherCondition(dto.getWeatherCondition());
        record.setMoonPhase(dto.getMoonPhase());
        record.setOutcome(dto.getOutcome());
        record.setPhotoUrl(dto.getPhotoUrl());
        record.setNotes(dto.getNotes());

        return convertToResponseDTO(catchRecordRepository.save(record));
    }

    /**
     * Dois caminhos para o ponto de pesca, decididos por fishingSpotId.
     *
     * <p>Sem o id, a coordenada e o ponto: procura-se um ponto ja registrado no mesmo
     * rio a poucas dezenas de metros e, so quando nao existe nenhum, cria-se um novo na
     * mesma transacao do registro. Sem essa busca, cada captura na mesma pedra criava
     * mais uma linha, e a tabela crescia uma vez por peixe em vez de uma vez por lugar.
     */
    private FishingSpot resolveFishingSpot(CatchRecordRequestDTO dto) {
        if (dto.getFishingSpotId() != null) {
            return fishingSpotRepository.findById(dto.getFishingSpotId())
                    .orElseThrow(() -> new NotFoundException("Ponto de pesca não encontrado."));
        }

        if (dto.getRiverId() == null || dto.getLatitude() == null || dto.getLongitude() == null) {
            throw new BusinessException(
                    "Envie um fishingSpotId, ou o rio e a coordenada do ponto no mapa.");
        }

        River rio = riverRepository.findById(dto.getRiverId())
                .orElseThrow(() -> new NotFoundException("Rio não encontrado para associar ao ponto."));

        List<FishingSpot> nearby = fishingSpotRepository.findNearby(
                rio.getId(),
                dto.getLatitude() - SAME_SPOT_DEGREES,
                dto.getLatitude() + SAME_SPOT_DEGREES,
                dto.getLongitude() - SAME_SPOT_DEGREES,
                dto.getLongitude() + SAME_SPOT_DEGREES);

        if (!nearby.isEmpty()) {
            return nearby.get(0);
        }

        FishingSpot newSpot = new FishingSpot();
        newSpot.setRiver(rio);
        newSpot.setLatitude(dto.getLatitude());
        newSpot.setLongitude(dto.getLongitude());

        String nomeDoLocal = (dto.getSpotName() != null && !dto.getSpotName().trim().isEmpty())
                ? dto.getSpotName().trim()
                : "Ponto no " + rio.getName();

        newSpot.setName(nomeDoLocal);
        newSpot.setAccessType("Não especificado");

        return fishingSpotRepository.save(newSpot);
    }

    @Transactional
    public void delete(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        CatchRecord record = catchRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Registro de captura não encontrado."));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> RoleName.ROLE_ADMIN.name().equals(authority.getAuthority()));

        boolean isOwner = record.getUser() != null
                && record.getUser().getEmail().equals(authentication.getName());

        // Um registro de outra pessoa responde igual a um registro inexistente: um 403
        // confirmaria a existência do registro para quem estiver testando ids.
        if (!isOwner && !isAdmin) {
            throw new NotFoundException("Registro de captura não encontrado.");
        }

        catchRecordRepository.delete(record);
    }

    private CatchRecordResponseDTO convertToResponseDTO(CatchRecord record) {
        CatchRecordResponseDTO dto = new CatchRecordResponseDTO();
        dto.setId(record.getId());

        if (record.getUser() != null) {
            dto.setUserName(record.getUser().getName());
        }
        if (record.getFish() != null) {
            dto.setFishId(record.getFish().getId());
            dto.setFishName(record.getFish().getCommonName());
        }
        if (record.getFishingSpot() != null) {
            dto.setFishingSpotId(record.getFishingSpot().getId());
            dto.setFishingSpotName(record.getFishingSpot().getName());
        }
        if (record.getBait() != null) {
            dto.setBaitId(record.getBait().getId());
            dto.setBaitName(record.getBait().getName());
        }
        if (record.getEquipment() != null) {
            dto.setEquipmentId(record.getEquipment().getId());
            // Antes so o id vinha, enquanto isca, peixe e ponto traziam id e nome. O
            // cliente ficava obrigado a buscar a lista inteira de equipamentos so para
            // exibir uma linha do diario.
            dto.setEquipmentType(record.getEquipment().getType());
        }

        dto.setWeightInKg(record.getWeightInKg());
        dto.setLengthInCm(record.getLengthInCm());
        dto.setCatchDate(record.getCatchDate());
        dto.setWeatherCondition(record.getWeatherCondition());
        dto.setMoonPhase(record.getMoonPhase());
        dto.setOutcome(record.getOutcome());
        dto.setPhotoUrl(record.getPhotoUrl());
        dto.setNotes(record.getNotes());

        return dto;
    }

    private List<RankingPeixeResponseDTO> mapearRankingPeixe(List<CatchRecord> peixes, boolean isComprimento) {
        List<RankingPeixeResponseDTO> ranking = new ArrayList<>();
        int posicao = 1;
        for (CatchRecord record : peixes) {
            String pescador = record.getUser() != null ? record.getUser().getName() : "Desconhecido";
            String especie = record.getFish() != null ? record.getFish().getCommonName() : "Espécie Não Informada";
            String local = record.getFishingSpot() != null ? record.getFishingSpot().getName() : "Desconhecido";
            Double medida = isComprimento ? record.getLengthInCm() : record.getWeightInKg();

            ranking.add(new RankingPeixeResponseDTO(posicao++, pescador, especie, local, medida, record.getPhotoUrl()));
        }
        return ranking;
    }

    public List<RankingPeixeResponseDTO> getRankingPorComprimento() {
        return mapearRankingPeixe(catchRecordRepository.findTop10ByLengthInCmIsNotNullOrderByLengthInCmDesc(), true);
    }

    public List<RankingPeixeResponseDTO> getRankingPorPeso() {
        return mapearRankingPeixe(catchRecordRepository.findTop10ByWeightInKgIsNotNullOrderByWeightInKgDesc(), false);
    }

    public List<RankingPescadorResponseDTO> getRankingPescadores() {
        List<RankingPescadorProjection> projecoes =
                catchRecordRepository.findTopPescadores(PageRequest.of(0, RANKING_SIZE));

        List<RankingPescadorResponseDTO> ranking = new ArrayList<>();
        int posicao = 1;
        for (RankingPescadorProjection proj : projecoes) {
            ranking.add(new RankingPescadorResponseDTO(posicao++, proj.getPescador(), proj.getCapturas(), proj.getDiasNaAgua()));
        }
        return ranking;
    }
}
