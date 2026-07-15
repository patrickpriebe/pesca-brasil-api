package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.RiverSpeciesRequestDTO;
import com.fishing.brazil.dto.response.RiverSpeciesResponseDTO;
import com.fishing.brazil.entity.Fish;
import com.fishing.brazil.entity.River;
import com.fishing.brazil.entity.RiverSpecies;
import com.fishing.brazil.repository.FishRepository;
import com.fishing.brazil.repository.RiverRepository;
import com.fishing.brazil.repository.RiverSpeciesRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RiverSpeciesService {

    private final RiverSpeciesRepository riverSpeciesRepository;
    private final RiverRepository riverRepository;
    private final FishRepository fishRepository;

    public RiverSpeciesService(RiverSpeciesRepository riverSpeciesRepository, RiverRepository riverRepository, FishRepository fishRepository) {
        this.riverSpeciesRepository = riverSpeciesRepository;
        this.riverRepository = riverRepository;
        this.fishRepository = fishRepository;
    }

    public Page<RiverSpeciesResponseDTO> findAll(Pageable pageable) {
        return riverSpeciesRepository.findAll(pageable).map(this::convertToResponseDTO);
    }

    public Page<RiverSpeciesResponseDTO> findByRiverId(Long riverId, Pageable pageable) {
        return riverSpeciesRepository.findByRiverId(riverId, pageable).map(this::convertToResponseDTO);
    }

    public RiverSpeciesResponseDTO save(RiverSpeciesRequestDTO dto) {
        River river = riverRepository.findById(dto.getRiverId())
                .orElseThrow(() -> new RuntimeException("Rio não encontrado"));
        Fish fish = fishRepository.findById(dto.getFishId())
                .orElseThrow(() -> new RuntimeException("Peixe não encontrado"));

        RiverSpecies rs = new RiverSpecies();
        rs.setRiver(river);
        rs.setFish(fish);
        rs.setAbundance(dto.getAbundance());
        rs.setBestSeason(dto.getBestSeason());

        return convertToResponseDTO(riverSpeciesRepository.save(rs));
    }

    public void delete(Long id) {
        riverSpeciesRepository.deleteById(id);
    }

    private RiverSpeciesResponseDTO convertToResponseDTO(RiverSpecies rs) {
        RiverSpeciesResponseDTO dto = new RiverSpeciesResponseDTO();
        dto.setId(rs.getId());
        dto.setAbundance(rs.getAbundance());
        dto.setBestSeason(rs.getBestSeason());
        if(rs.getRiver() != null) {
            dto.setRiverId(rs.getRiver().getId());
            dto.setRiverName(rs.getRiver().getName());
        }
        if(rs.getFish() != null) {
            dto.setFishId(rs.getFish().getId());
            dto.setFishCommonName(rs.getFish().getCommonName());
        }
        return dto;
    }
}