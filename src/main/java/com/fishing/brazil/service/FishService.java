package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.FishRequestDTO;
import com.fishing.brazil.dto.response.FishResponseDTO;
import com.fishing.brazil.entity.Bait;
import com.fishing.brazil.entity.Equipment;
import com.fishing.brazil.entity.Fish;
import com.fishing.brazil.repository.BaitRepository;
import com.fishing.brazil.repository.EquipmentRepository;
import com.fishing.brazil.repository.FishRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FishService {

    private final FishRepository fishRepository;
    private final BaitRepository baitRepository;
    private final EquipmentRepository equipmentRepository;

    public FishService(FishRepository fishRepository,
                       BaitRepository baitRepository,
                       EquipmentRepository equipmentRepository) {
        this.fishRepository = fishRepository;
        this.baitRepository = baitRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public Page<FishResponseDTO> findFishes(String name, Pageable pageable) {
        Page<Fish> fishPage;

        if (name != null && !name.trim().isEmpty()) {
            fishPage = fishRepository.findByCommonNameContainingIgnoreCase(name, pageable);
        } else {
            fishPage = fishRepository.findAll(pageable);
        }

        return fishPage.map(this::convertToResponseDTO);
    }

    public Optional<FishResponseDTO> findById(Long id) {
        return fishRepository.findById(id)
                .map(this::convertToResponseDTO);
    }

    public FishResponseDTO save(FishRequestDTO dto) {
        Fish fish = new Fish();
        fish.setCommonName(dto.getCommonName());
        fish.setScientificName(dto.getScientificName());
        fish.setConservationStatus(dto.getConservationStatus());
        fish.setDescription(dto.getDescription());
        fish.setImageUrl(dto.getImageUrl());

        if (dto.getRecommendedBaitIds() != null && !dto.getRecommendedBaitIds().isEmpty()) {
            List<Bait> iscas = baitRepository.findAllById(dto.getRecommendedBaitIds());
            fish.setRecommendedBaits(iscas);
        }

        if (dto.getRecommendedEquipmentIds() != null && !dto.getRecommendedEquipmentIds().isEmpty()) {
            List<Equipment> equipamentos = equipmentRepository.findAllById(dto.getRecommendedEquipmentIds());
            fish.setRecommendedEquipments(equipamentos);
        }

        Fish savedFish = fishRepository.save(fish);
        return convertToResponseDTO(savedFish);
    }

    public void delete(Long id) {
        fishRepository.deleteById(id);
    }

    private FishResponseDTO convertToResponseDTO(Fish fish) {
        FishResponseDTO dto = new FishResponseDTO();
        dto.setId(fish.getId());
        dto.setCommonName(fish.getCommonName());
        dto.setScientificName(fish.getScientificName());
        dto.setConservationStatus(fish.getConservationStatus());
        dto.setDescription(fish.getDescription());
        dto.setImageUrl(fish.getImageUrl());

        return dto;
    }
}