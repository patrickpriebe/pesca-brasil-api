package com.fishing.brazil.service;

import com.fishing.brazil.dto.request.EquipmentRequestDTO;
import com.fishing.brazil.dto.response.EquipmentResponseDTO;
import com.fishing.brazil.entity.Equipment;
import com.fishing.brazil.repository.EquipmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public Page<EquipmentResponseDTO> findAll(Pageable pageable) {
        return equipmentRepository.findAll(pageable).map(this::convertToResponseDTO);
    }

    public Optional<EquipmentResponseDTO> findById(Long id) {
        return equipmentRepository.findById(id).map(this::convertToResponseDTO);
    }

    public EquipmentResponseDTO save(EquipmentRequestDTO dto) {
        Equipment equipment = new Equipment();
        equipment.setType(dto.getType());
        equipment.setRecommendedLineWeight(dto.getRecommendedLineWeight());
        equipment.setAction(dto.getAction());
        return convertToResponseDTO(equipmentRepository.save(equipment));
    }

    public void delete(Long id) {
        equipmentRepository.deleteById(id);
    }

    private EquipmentResponseDTO convertToResponseDTO(Equipment equipment) {
        EquipmentResponseDTO dto = new EquipmentResponseDTO();
        dto.setId(equipment.getId());
        dto.setType(equipment.getType());
        dto.setRecommendedLineWeight(equipment.getRecommendedLineWeight());
        dto.setAction(equipment.getAction());
        return dto;
    }
}