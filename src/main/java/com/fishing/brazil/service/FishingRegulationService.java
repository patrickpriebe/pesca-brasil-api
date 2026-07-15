package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.FishingRegulationRequestDTO;
import com.fishing.brazil.dto.response.FishingRegulationResponseDTO;
import com.fishing.brazil.entity.FishingRegulation;
import com.fishing.brazil.repository.FishingRegulationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FishingRegulationService {

    private final FishingRegulationRepository repository;

    public FishingRegulationService(FishingRegulationRepository repository) {
        this.repository = repository;
    }

    public Page<FishingRegulationResponseDTO> findRegulations(String basin, Pageable pageable) {
        Page<FishingRegulation> pageData;
        if (basin != null && !basin.trim().isEmpty()) {
            pageData = repository.findByHydrographicBasinContainingIgnoreCase(basin, pageable);
        } else {
            pageData = repository.findAll(pageable);
        }
        return pageData.map(this::convertToResponseDTO);
    }

    public FishingRegulationResponseDTO save(FishingRegulationRequestDTO dto) {
        FishingRegulation reg = new FishingRegulation();
        reg.setHydrographicBasin(dto.getHydrographicBasin());
        reg.setStartDate(dto.getStartDate());
        reg.setEndDate(dto.getEndDate());
        reg.setNotes(dto.getNotes());
        return convertToResponseDTO(repository.save(reg));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private FishingRegulationResponseDTO convertToResponseDTO(FishingRegulation reg) {
        FishingRegulationResponseDTO dto = new FishingRegulationResponseDTO();
        dto.setId(reg.getId());
        dto.setHydrographicBasin(reg.getHydrographicBasin());
        dto.setStartDate(reg.getStartDate());
        dto.setEndDate(reg.getEndDate());
        dto.setNotes(reg.getNotes());
        return dto;
    }
}