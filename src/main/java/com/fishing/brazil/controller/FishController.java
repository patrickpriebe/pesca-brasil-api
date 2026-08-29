package com.fishing.brazil.controller;

import com.fishing.brazil.config.web.PageableFactory;
import com.fishing.brazil.dto.request.FishRequestDTO;
import com.fishing.brazil.dto.response.FishResponseDTO;
import com.fishing.brazil.exception.NotFoundException;
import com.fishing.brazil.service.FishService;
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

@Tag(name = "Peixes", description = "Catálogo de espécies. Leitura pública, escrita restrita a ROLE_ADMIN")
@RestController
@RequestMapping("/api/fishes")
public class FishController {

    private static final Set<String> SORTABLE =
            Set.of("commonName", "scientificName", "conservationStatus", "id");

    private final FishService fishService;

    public FishController(FishService fishService) {
        this.fishService = fishService;
    }

    @Operation(summary = "Lista espécies", description = "Filtra por nome comum com ?name=")
    @GetMapping
    public ResponseEntity<Page<FishResponseDTO>> getFishes(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "commonName") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "commonName", Sort.Direction.ASC);
        return ResponseEntity.ok(fishService.findFishes(name, pageable));
    }

    @Operation(summary = "Busca uma espécie pelo id",
            description = "Inclui as iscas e os equipamentos recomendados")
    @GetMapping("/{id}")
    public ResponseEntity<FishResponseDTO> getFishById(@PathVariable Long id) {
        return ResponseEntity.ok(fishService.findById(id)
                .orElseThrow(() -> new NotFoundException("Peixe não encontrado.")));
    }

    @Operation(summary = "Cadastra uma espécie", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FishResponseDTO> createFish(@Valid @RequestBody FishRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fishService.save(dto));
    }

    @Operation(summary = "Remove uma espécie", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFish(@PathVariable Long id) {
        fishService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
