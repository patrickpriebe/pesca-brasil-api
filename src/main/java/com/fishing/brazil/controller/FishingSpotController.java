package com.fishing.brazil.controller;

import com.fishing.brazil.config.web.PageableFactory;
import com.fishing.brazil.dto.request.FishingSpotRequestDTO;
import com.fishing.brazil.dto.response.FishingSpotResponseDTO;
import com.fishing.brazil.exception.NotFoundException;
import com.fishing.brazil.service.FishingSpotService;
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

@Tag(name = "Pontos de pesca",
        description = "Pontos curados. O outro caminho para esta tabela é POST /api/catch-records")
@RestController
@RequestMapping("/api/fishing-spots")
public class FishingSpotController {

    private static final Set<String> SORTABLE = Set.of("name", "accessType", "id");

    private final FishingSpotService fishingSpotService;

    public FishingSpotController(FishingSpotService fishingSpotService) {
        this.fishingSpotService = fishingSpotService;
    }

    @Operation(summary = "Lista pontos de pesca")
    @GetMapping
    public ResponseEntity<Page<FishingSpotResponseDTO>> getAllFishingSpots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "name", Sort.Direction.ASC);
        return ResponseEntity.ok(fishingSpotService.findAll(pageable));
    }

    @Operation(summary = "Pontos de um rio")
    @GetMapping("/river/{riverId}")
    public ResponseEntity<Page<FishingSpotResponseDTO>> getSpotsByRiver(
            @PathVariable Long riverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "name", Sort.Direction.ASC);
        return ResponseEntity.ok(fishingSpotService.findByRiverId(riverId, pageable));
    }

    @Operation(summary = "Busca um ponto pelo id")
    @GetMapping("/{id}")
    public ResponseEntity<FishingSpotResponseDTO> getSpotById(@PathVariable Long id) {
        return ResponseEntity.ok(fishingSpotService.findById(id)
                .orElseThrow(() -> new NotFoundException("Ponto de pesca não encontrado.")));
    }

    @Operation(summary = "Cadastra um ponto de pesca", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FishingSpotResponseDTO> createSpot(@Valid @RequestBody FishingSpotRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fishingSpotService.save(dto));
    }

    @Operation(summary = "Remove um ponto de pesca", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpot(@PathVariable Long id) {
        fishingSpotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
