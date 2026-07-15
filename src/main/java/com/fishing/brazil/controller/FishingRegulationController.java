package com.fishing.brazil.controller;

import com.fishing.brazil.dto.request.FishingRegulationRequestDTO;
import com.fishing.brazil.dto.response.FishingRegulationResponseDTO;
import com.fishing.brazil.service.FishingRegulationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fishing-regulations")
public class FishingRegulationController {

    private final FishingRegulationService service;

    public FishingRegulationController(FishingRegulationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<FishingRegulationResponseDTO>> getRegulations(
            @RequestParam(required = false) String basin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "hydrographicBasin") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(service.findRegulations(basin, pageable));
    }

    @PostMapping
    public ResponseEntity<FishingRegulationResponseDTO> createRegulation(@RequestBody FishingRegulationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegulation(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}