package com.fishing.brazil.controller;

import com.fishing.brazil.dto.request.EquipmentRequestDTO;
import com.fishing.brazil.dto.response.EquipmentResponseDTO;
import com.fishing.brazil.service.EquipmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public ResponseEntity<Page<EquipmentResponseDTO>> getAllEquipments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(equipmentService.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<EquipmentResponseDTO> createEquipment(@RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}