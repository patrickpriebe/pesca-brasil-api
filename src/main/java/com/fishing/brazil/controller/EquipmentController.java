package com.fishing.brazil.controller;

import com.fishing.brazil.config.web.PageableFactory;
import com.fishing.brazil.dto.request.EquipmentRequestDTO;
import com.fishing.brazil.dto.response.EquipmentResponseDTO;
import com.fishing.brazil.exception.NotFoundException;
import com.fishing.brazil.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Equipamentos", description = "Catálogo de equipamentos. Leitura pública, escrita restrita a ROLE_ADMIN")
@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private static final Set<String> SORTABLE = Set.of("type", "recommendedLineWeight", "action", "id");

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @Operation(summary = "Lista equipamentos")
    @GetMapping
    public ResponseEntity<Page<EquipmentResponseDTO>> getAllEquipments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "id", Sort.Direction.ASC);
        return ResponseEntity.ok(equipmentService.findAll(pageable));
    }

    @Operation(summary = "Busca um equipamento pelo id")
    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponseDTO> getEquipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipamento não encontrado.")));
    }

    @Operation(summary = "Cadastra um equipamento", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EquipmentResponseDTO> createEquipment(@Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.save(dto));
    }

    @Operation(summary = "Remove um equipamento", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
