package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.FishRequestDTO;
import com.fishing.brazil.dto.response.BaitResponseDTO;
import com.fishing.brazil.dto.response.EquipmentResponseDTO;
import com.fishing.brazil.dto.response.FishResponseDTO;
import com.fishing.brazil.entity.Bait;
import com.fishing.brazil.entity.Equipment;
import com.fishing.brazil.entity.Fish;
import com.fishing.brazil.exception.NotFoundException;
import com.fishing.brazil.repository.BaitRepository;
import com.fishing.brazil.repository.EquipmentRepository;
import com.fishing.brazil.repository.FishRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
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
        return fishRepository.findById(id).map(this::convertToResponseDTO);
    }

    @Transactional
    public FishResponseDTO save(FishRequestDTO dto) {
        Fish fish = new Fish();
        fish.setCommonName(dto.getCommonName());
        fish.setScientificName(dto.getScientificName());
        fish.setConservationStatus(dto.getConservationStatus());
        fish.setDescription(dto.getDescription());
        fish.setImageUrl(dto.getImageUrl());

        fish.setRecommendedBaits(resolveBaits(dto.getRecommendedBaitIds()));
        fish.setRecommendedEquipments(resolveEquipments(dto.getRecommendedEquipmentIds()));

        return convertToResponseDTO(fishRepository.save(fish));
    }

    /**
     * findAllById devolve o que encontrar e ignora o resto, entao um id inexistente
     * na lista sumia sem nenhum sinal: o cliente recebia 201 e uma recomendacao a
     * menos do que pediu. Conferir a contagem transforma isso numa recusa.
     */
    private List<Bait> resolveBaits(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Bait> found = baitRepository.findAllById(ids);
        if (found.size() != ids.stream().distinct().count()) {
            throw new NotFoundException("Uma ou mais iscas informadas não existem.");
        }
        return found;
    }

    private List<Equipment> resolveEquipments(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Equipment> found = equipmentRepository.findAllById(ids);
        if (found.size() != ids.stream().distinct().count()) {
            throw new NotFoundException("Um ou mais equipamentos informados não existem.");
        }
        return found;
    }

    @Transactional
    public void delete(Long id) {
        if (!fishRepository.existsById(id)) {
            throw new NotFoundException("Peixe não encontrado.");
        }
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

        if (fish.getRecommendedBaits() != null) {
            dto.setRecommendedBaits(fish.getRecommendedBaits().stream()
                    .map(bait -> {
                        BaitResponseDTO baitDto = new BaitResponseDTO();
                        baitDto.setId(bait.getId());
                        baitDto.setName(bait.getName());
                        baitDto.setType(bait.getType());
                        baitDto.setDescription(bait.getDescription());
                        return baitDto;
                    })
                    .collect(Collectors.toList()));
        }

        if (fish.getRecommendedEquipments() != null) {
            dto.setRecommendedEquipments(fish.getRecommendedEquipments().stream()
                    .map(equipment -> {
                        EquipmentResponseDTO equipmentDto = new EquipmentResponseDTO();
                        equipmentDto.setId(equipment.getId());
                        equipmentDto.setType(equipment.getType());
                        equipmentDto.setRecommendedLineWeight(equipment.getRecommendedLineWeight());
                        equipmentDto.setAction(equipment.getAction());
                        return equipmentDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
