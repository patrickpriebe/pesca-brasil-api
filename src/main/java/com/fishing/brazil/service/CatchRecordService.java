package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.CatchRecordRequestDTO;
import com.fishing.brazil.dto.response.CatchRecordResponseDTO;
import com.fishing.brazil.dto.response.ranking.RankingPeixeResponseDTO;
import com.fishing.brazil.dto.response.ranking.RankingPescadorResponseDTO;
import com.fishing.brazil.entity.CatchRecord;
import com.fishing.brazil.entity.FishingSpot;
import com.fishing.brazil.entity.login.User;
import com.fishing.brazil.repository.*;
import com.fishing.brazil.repository.login.UserRepository;
import com.fishing.brazil.repository.projection.RankingPescadorProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatchRecordService {

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

    public Page<CatchRecordResponseDTO> findAll(Pageable pageable) {
        return catchRecordRepository.findAll(pageable).map(this::convertToResponseDTO);
    }

    public CatchRecordResponseDTO save(CatchRecordRequestDTO dto) {
        String emailDoPescador = SecurityContextHolder.getContext().getAuthentication().getName();
        User pescadorLogado = userRepository.findByEmail(emailDoPescador)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado no banco."));

        CatchRecord record = new CatchRecord();
        record.setUser(pescadorLogado);

        record.setFish(fishRepository.findById(dto.getFishId())
                .orElseThrow(() -> new RuntimeException("Peixe não encontrado.")));

        if (dto.getFishingSpotId() == null && dto.getRiverId() != null && dto.getLatitude() != null) {
            FishingSpot newSpot = new FishingSpot();

            var rio = riverRepository.findById(dto.getRiverId())
                    .orElseThrow(() -> new RuntimeException("Rio não encontrado para associar ao ponto."));

            newSpot.setRiver(rio);
            newSpot.setLatitude(dto.getLatitude());
            newSpot.setLongitude(dto.getLongitude());

            String nomeDoLocal = (dto.getSpotName() != null && !dto.getSpotName().trim().isEmpty())
                    ? dto.getSpotName()
                    : "Ponto no " + rio.getName();

            newSpot.setName(nomeDoLocal);
            newSpot.setAccessType("Não especificado");

            FishingSpot spotSalvo = fishingSpotRepository.save(newSpot);
            record.setFishingSpot(spotSalvo);
        } else {
            record.setFishingSpot(fishingSpotRepository.findById(dto.getFishingSpotId())
                    .orElseThrow(() -> new RuntimeException("Ponto de pesca não especificado ou não encontrado.")));
        }
        // ------------------------------------------------------

        if (dto.getBaitId() != null) {
            record.setBait(baitRepository.findById(dto.getBaitId())
                    .orElseThrow(() -> new RuntimeException("Isca não encontrada.")));
        }

        if (dto.getEquipmentId() != null) {
            record.setEquipment(equipmentRepository.findById(dto.getEquipmentId())
                    .orElseThrow(() -> new RuntimeException("Equipamento não encontrado.")));
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

    public void delete(Long id) {
        catchRecordRepository.deleteById(id);
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
        List<RankingPeixeResponseDTO> ranking = new java.util.ArrayList<>();
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
        List<CatchRecord> peixes = catchRecordRepository.findTop10ByLengthInCmIsNotNullOrderByLengthInCmDesc();
        return mapearRankingPeixe(peixes, true);
    }

    public List<RankingPeixeResponseDTO> getRankingPorPeso() {
        List<CatchRecord> peixes = catchRecordRepository.findTop10ByWeightInKgIsNotNullOrderByWeightInKgDesc();
        return mapearRankingPeixe(peixes, false);
    }

    public List<RankingPescadorResponseDTO> getRankingPescadores() {
        List<RankingPescadorProjection> projecoes = catchRecordRepository.findTopPescadores(org.springframework.data.domain.PageRequest.of(0, 10));

        List<RankingPescadorResponseDTO> ranking = new java.util.ArrayList<>();
        int posicao = 1;
        for (RankingPescadorProjection proj : projecoes) {
            ranking.add(new RankingPescadorResponseDTO(posicao++, proj.getPescador(), proj.getCapturas(), proj.getDiasNaAgua()));
        }
        return ranking;
    }
}