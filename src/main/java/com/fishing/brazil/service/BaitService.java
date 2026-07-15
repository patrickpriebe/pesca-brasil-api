package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.BaitRequestDTO;
import com.fishing.brazil.dto.response.BaitResponseDTO;
import com.fishing.brazil.entity.Bait;
import com.fishing.brazil.repository.BaitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BaitService {

    private final BaitRepository baitRepository;

    public BaitService(BaitRepository baitRepository) {
        this.baitRepository = baitRepository;
    }

    public Page<BaitResponseDTO> findBaits(String name, Pageable pageable) {
        Page<Bait> baitPage;
        if (name != null && !name.trim().isEmpty()) {
            baitPage = baitRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            baitPage = baitRepository.findAll(pageable);
        }
        return baitPage.map(this::convertToResponseDTO);
    }

    public Optional<BaitResponseDTO> findById(Long id) {
        return baitRepository.findById(id).map(this::convertToResponseDTO);
    }

    public BaitResponseDTO save(BaitRequestDTO dto) {
        Bait bait = new Bait();
        bait.setName(dto.getName());
        bait.setType(dto.getType());
        bait.setDescription(dto.getDescription());
        return convertToResponseDTO(baitRepository.save(bait));
    }

    public void delete(Long id) {
        baitRepository.deleteById(id);
    }

    private BaitResponseDTO convertToResponseDTO(Bait bait) {
        BaitResponseDTO dto = new BaitResponseDTO();
        dto.setId(bait.getId());
        dto.setName(bait.getName());
        dto.setType(bait.getType());
        dto.setDescription(bait.getDescription());
        return dto;
    }
}