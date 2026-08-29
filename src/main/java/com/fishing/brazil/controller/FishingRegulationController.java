package com.fishing.brazil.controller;

import com.fishing.brazil.config.web.PageableFactory;
import com.fishing.brazil.dto.request.FishingRegulationRequestDTO;
import com.fishing.brazil.dto.response.FishingRegulationResponseDTO;
import com.fishing.brazil.service.FishingRegulationService;
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

@Tag(name = "Defesos",
        description = "Períodos de piracema por bacia hidrográfica. A API os publica; não os aplica")
@RestController
@RequestMapping("/api/fishing-regulations")
public class FishingRegulationController {

    private static final Set<String> SORTABLE = Set.of("hydrographicBasin", "startDate", "endDate", "id");

    private final FishingRegulationService service;

    public FishingRegulationController(FishingRegulationService service) {
        this.service = service;
    }

    @Operation(summary = "Lista os defesos", description = "Filtra por bacia com ?basin=")
    @GetMapping
    public ResponseEntity<Page<FishingRegulationResponseDTO>> getRegulations(
            @RequestParam(required = false) String basin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "hydrographicBasin") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "hydrographicBasin", Sort.Direction.ASC);
        return ResponseEntity.ok(service.findRegulations(basin, pageable));
    }

    @Operation(summary = "Cadastra um defeso", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FishingRegulationResponseDTO> createRegulation(
            @Valid @RequestBody FishingRegulationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @Operation(summary = "Remove um defeso", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegulation(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
