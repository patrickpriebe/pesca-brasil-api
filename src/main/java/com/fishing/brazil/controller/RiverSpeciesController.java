package com.fishing.brazil.controller;

import com.fishing.brazil.dto.request.RiverSpeciesRequestDTO;
import com.fishing.brazil.dto.response.RiverSpeciesResponseDTO;
import com.fishing.brazil.service.RiverSpeciesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/river-species")
public class RiverSpeciesController {

    private final RiverSpeciesService riverSpeciesService;

    public RiverSpeciesController(RiverSpeciesService riverSpeciesService) {
        this.riverSpeciesService = riverSpeciesService;
    }

    @GetMapping
    public ResponseEntity<Page<RiverSpeciesResponseDTO>> getAllRiverSpecies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(riverSpeciesService.findAll(pageable));
    }

    @GetMapping("/river/{riverId}")
    public ResponseEntity<Page<RiverSpeciesResponseDTO>> getByRiver(
            @PathVariable Long riverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(riverSpeciesService.findByRiverId(riverId, pageable));
    }

    @PostMapping
    public ResponseEntity<RiverSpeciesResponseDTO> createRiverSpecies(@RequestBody RiverSpeciesRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riverSpeciesService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRiverSpecies(@PathVariable Long id) {
        riverSpeciesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}