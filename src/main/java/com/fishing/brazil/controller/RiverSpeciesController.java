package com.fishing.brazil.controller;

import com.fishing.brazil.config.web.PageableFactory;
import com.fishing.brazil.dto.request.RiverSpeciesRequestDTO;
import com.fishing.brazil.dto.response.RiverSpeciesResponseDTO;
import com.fishing.brazil.service.RiverSpeciesService;
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

@Tag(name = "Espécies por rio",
        description = "Qual espécie vive em qual rio, com abundância e melhor época")
@RestController
@RequestMapping("/api/river-species")
public class RiverSpeciesController {

    private static final Set<String> SORTABLE = Set.of("id", "abundance", "bestSeason");

    private final RiverSpeciesService riverSpeciesService;

    public RiverSpeciesController(RiverSpeciesService riverSpeciesService) {
        this.riverSpeciesService = riverSpeciesService;
    }

    @Operation(summary = "Lista as associações entre rios e espécies")
    @GetMapping
    public ResponseEntity<Page<RiverSpeciesResponseDTO>> getAllRiverSpecies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "id", Sort.Direction.ASC);
        return ResponseEntity.ok(riverSpeciesService.findAll(pageable));
    }

    @Operation(summary = "Espécies de um rio")
    @GetMapping("/river/{riverId}")
    public ResponseEntity<Page<RiverSpeciesResponseDTO>> getByRiver(
            @PathVariable Long riverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "id", Sort.Direction.ASC);
        return ResponseEntity.ok(riverSpeciesService.findByRiverId(riverId, pageable));
    }

    @Operation(summary = "Associa uma espécie a um rio",
            description = "O par rio + peixe é único: repetir a associação responde 409.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RiverSpeciesResponseDTO> createRiverSpecies(@Valid @RequestBody RiverSpeciesRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riverSpeciesService.save(dto));
    }

    @Operation(summary = "Remove a associação", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRiverSpecies(@PathVariable Long id) {
        riverSpeciesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
