package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.FishingSpotRequestDTO;
import com.fishing.brazil.dto.response.FishingSpotResponseDTO;
import com.fishing.brazil.entity.FishingSpot;
import com.fishing.brazil.entity.River;
import com.fishing.brazil.repository.FishingSpotRepository;
import com.fishing.brazil.repository.RiverRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FishingSpotService {

    private final FishingSpotRepository fishingSpotRepository;
    private final RiverRepository riverRepository;

    public FishingSpotService(FishingSpotRepository fishingSpotRepository, RiverRepository riverRepository) {
        this.fishingSpotRepository = fishingSpotRepository;
        this.riverRepository = riverRepository;
    }

    public Page<FishingSpotResponseDTO> findAll(Pageable pageable) {
        return fishingSpotRepository.findAll(pageable).map(this::convertToResponseDTO);
    }

    public Optional<FishingSpotResponseDTO> findById(Long id) {
        return fishingSpotRepository.findById(id).map(this::convertToResponseDTO);
    }

    public Page<FishingSpotResponseDTO> findByRiverId(Long riverId, Pageable pageable) {
        return fishingSpotRepository.findByRiverId(riverId, pageable).map(this::convertToResponseDTO);
    }

    public FishingSpotResponseDTO save(FishingSpotRequestDTO dto) {
        River river = riverRepository.findById(dto.getRiverId())
                .orElseThrow(() -> new RuntimeException("Rio não encontrado com o ID: " + dto.getRiverId()));

        FishingSpot spot = new FishingSpot();
        spot.setRiver(river);
        spot.setName(dto.getName());
        spot.setLatitude(dto.getLatitude());
        spot.setLongitude(dto.getLongitude());
        spot.setAccessType(dto.getAccessType());
        spot.setDescription(dto.getDescription());

        return convertToResponseDTO(fishingSpotRepository.save(spot));
    }

    public void delete(Long id) {
        fishingSpotRepository.deleteById(id);
    }

    private FishingSpotResponseDTO convertToResponseDTO(FishingSpot spot) {
        FishingSpotResponseDTO dto = new FishingSpotResponseDTO();
        dto.setId(spot.getId());
        dto.setName(spot.getName());
        dto.setLatitude(spot.getLatitude());
        dto.setLongitude(spot.getLongitude());
        dto.setAccessType(spot.getAccessType());
        dto.setDescription(spot.getDescription());

        if (spot.getRiver() != null) {
            dto.setRiverId(spot.getRiver().getId());
            dto.setRiverName(spot.getRiver().getName());
        }
        return dto;
    }
}