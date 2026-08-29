package com.fishing.brazil.controller;

import com.fishing.brazil.config.web.PageableFactory;
import com.fishing.brazil.dto.request.CatchRecordRequestDTO;
import com.fishing.brazil.dto.response.CatchRecordResponseDTO;
import com.fishing.brazil.dto.response.ranking.RankingPeixeResponseDTO;
import com.fishing.brazil.dto.response.ranking.RankingPescadorResponseDTO;
import com.fishing.brazil.service.CatchRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "Capturas", description = "O diário de pesca: registros, busca e rankings")
@RestController
@RequestMapping("/api/catch-records")
public class CatchRecordController {

    private static final Set<String> SORTABLE =
            Set.of("catchDate", "weightInKg", "lengthInCm", "id");

    private final CatchRecordService catchRecordService;

    public CatchRecordController(CatchRecordService catchRecordService) {
        this.catchRecordService = catchRecordService;
    }

    @Operation(summary = "Lista pública de capturas",
            description = "Aceita ?search= para filtrar por espécie ou nome do ponto de pesca.")
    @GetMapping
    public ResponseEntity<Page<CatchRecordResponseDTO>> getAllRecords(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "catchDate") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "catchDate", Sort.Direction.DESC);
        return ResponseEntity.ok(catchRecordService.findAll(search, pageable));
    }

    @Operation(summary = "As capturas de quem está autenticado",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/me")
    public ResponseEntity<Page<CatchRecordResponseDTO>> getMyRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "catchDate") String sortBy,
            @RequestParam(required = false) String direction
    ) {
        Pageable pageable = PageableFactory.of(page, size, sortBy, direction,
                SORTABLE, "catchDate", Sort.Direction.DESC);
        return ResponseEntity.ok(catchRecordService.findMine(pageable));
    }

    @Operation(summary = "Registra uma captura",
            description = "O dono vem do token: o corpo não tem campo de usuário. "
                    + "Envie fishingSpotId, ou riverId com latitude e longitude.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    public ResponseEntity<CatchRecordResponseDTO> createRecord(@Valid @RequestBody CatchRecordRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catchRecordService.save(dto));
    }

    @Operation(summary = "Remove uma captura própria",
            description = "Um registro de outra pessoa responde 404, igual a um inexistente.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        catchRecordService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ranking dos dez maiores peixes por comprimento")
    @GetMapping("/ranking/comprimento")
    public ResponseEntity<List<RankingPeixeResponseDTO>> getRankingPorComprimento() {
        return ResponseEntity.ok(catchRecordService.getRankingPorComprimento());
    }

    @Operation(summary = "Ranking dos dez maiores peixes por peso")
    @GetMapping("/ranking/peso")
    public ResponseEntity<List<RankingPeixeResponseDTO>> getRankingPorPeso() {
        return ResponseEntity.ok(catchRecordService.getRankingPorPeso());
    }

    @Operation(summary = "Ranking dos pescadores mais ativos")
    @GetMapping("/ranking/pescadores")
    public ResponseEntity<List<RankingPescadorResponseDTO>> getRankingDePescadores() {
        return ResponseEntity.ok(catchRecordService.getRankingPescadores());
    }
}
