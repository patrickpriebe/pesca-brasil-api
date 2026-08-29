package com.fishing.brazil.controller;

import com.fishing.brazil.config.web.PageableFactory;
import com.fishing.brazil.dto.request.RiverRequestDTO;
import com.fishing.brazil.dto.response.RiverResponseDTO;
import com.fishing.brazil.exception.NotFoundException;
import com.fishing.brazil.service.RiverService;
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

@Tag(name = "Rios", description = "Rios e bacias hidrográficas. Leitura pública, escrita restrita a ROLE_ADMIN")
@RestController
@RequestMapping("/api/rivers")
public class RiverController {

    private static final Set<String> SORTABLE = Set.of("name", "hydrographicBasin", "id");

    private final RiverService riverService;

    public RiverController(RiverService riverService) {
        this.riverService = riverService;
    }

    @Operation(summary = "Lista rios", description = "Filtra por nome com ?name=")
    @GetMapping
    public ResponseEntity<Page<RiverResponseDTO>> getRivers(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "name", Sort.Direction.ASC);
        return ResponseEntity.ok(riverService.findRivers(name, pageable));
    }

    @Operation(summary = "Busca um rio pelo id")
    @GetMapping("/{id}")
    public ResponseEntity<RiverResponseDTO> getRiverById(@PathVariable Long id) {
        return ResponseEntity.ok(riverService.findById(id)
                .orElseThrow(() -> new NotFoundException("Rio não encontrado.")));
    }

    @Operation(summary = "Cadastra um rio", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RiverResponseDTO> createRiver(@Valid @RequestBody RiverRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riverService.save(dto));
    }

    @Operation(summary = "Remove um rio", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRiver(@PathVariable Long id) {
        riverService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
