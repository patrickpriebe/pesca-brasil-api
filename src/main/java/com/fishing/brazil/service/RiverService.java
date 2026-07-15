package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.RiverRequestDTO;
import com.fishing.brazil.dto.response.RiverResponseDTO;
import com.fishing.brazil.entity.River;
import com.fishing.brazil.repository.RiverRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RiverService {

    private final RiverRepository riverRepository;

    public RiverService(RiverRepository riverRepository) {
        this.riverRepository = riverRepository;
    }

    public Page<RiverResponseDTO> findRivers(String name, Pageable pageable) {
        Page<River> riverPage;
        if (name != null && !name.trim().isEmpty()) {
            riverPage = riverRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            riverPage = riverRepository.findAll(pageable);
        }
        return riverPage.map(this::convertToResponseDTO);
    }

    public Optional<RiverResponseDTO> findById(Long id) {
        return riverRepository.findById(id).map(this::convertToResponseDTO);
    }

    public RiverResponseDTO save(RiverRequestDTO dto) {
        River river = new River();
        river.setName(dto.getName());
        river.setHydrographicBasin(dto.getHydrographicBasin());
        river.setDescription(dto.getDescription());
        return convertToResponseDTO(riverRepository.save(river));
    }

    public void delete(Long id) {
        riverRepository.deleteById(id);
    }

    private RiverResponseDTO convertToResponseDTO(River river) {
        RiverResponseDTO dto = new RiverResponseDTO();
        dto.setId(river.getId());
        dto.setName(river.getName());
        dto.setHydrographicBasin(river.getHydrographicBasin());
        dto.setDescription(river.getDescription());
        return dto;
    }
}