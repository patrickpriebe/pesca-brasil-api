package com.fishing.brazil.controller;

import com.fishing.brazil.config.web.PageableFactory;
import com.fishing.brazil.dto.request.BaitRequestDTO;
import com.fishing.brazil.dto.response.BaitResponseDTO;
import com.fishing.brazil.exception.NotFoundException;
import com.fishing.brazil.service.BaitService;
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

@Tag(name = "Iscas", description = "Catálogo de iscas. Leitura pública, escrita restrita a ROLE_ADMIN")
@RestController
@RequestMapping("/api/baits")
public class BaitController {

    private static final Set<String> SORTABLE = Set.of("name", "type", "id");

    private final BaitService baitService;

    public BaitController(BaitService baitService) {
        this.baitService = baitService;
    }

    @Operation(summary = "Lista iscas", description = "Filtra por nome com ?name=")
    @GetMapping
    public ResponseEntity<Page<BaitResponseDTO>> getBaits(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "name", Sort.Direction.ASC);
        return ResponseEntity.ok(baitService.findBaits(name, pageable));
    }

    @Operation(summary = "Busca uma isca pelo id")
    @GetMapping("/{id}")
    public ResponseEntity<BaitResponseDTO> getBaitById(@PathVariable Long id) {
        return ResponseEntity.ok(baitService.findById(id)
                .orElseThrow(() -> new NotFoundException("Isca não encontrada.")));
    }

    @Operation(summary = "Cadastra uma isca", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BaitResponseDTO> createBait(@Valid @RequestBody BaitRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(baitService.save(dto));
    }

    @Operation(summary = "Remove uma isca", security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBait(@PathVariable Long id) {
        baitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
