package com.fishing.brazil.controller;

import com.fishing.brazil.dto.request.FishingSpotRequestDTO;
import com.fishing.brazil.dto.response.FishingSpotResponseDTO;
import com.fishing.brazil.service.FishingSpotService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fishing-spots")
public class FishingSpotController {

    private final FishingSpotService fishingSpotService;

    public FishingSpotController(FishingSpotService fishingSpotService) {
        this.fishingSpotService = fishingSpotService;
    }

    @GetMapping
    public ResponseEntity<Page<FishingSpotResponseDTO>> getAllFishingSpots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(fishingSpotService.findAll(pageable));
    }

    @GetMapping("/river/{riverId}")
    public ResponseEntity<Page<FishingSpotResponseDTO>> getSpotsByRiver(
            @PathVariable Long riverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(fishingSpotService.findByRiverId(riverId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FishingSpotResponseDTO> getSpotById(@PathVariable Long id) {
        return fishingSpotService.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FishingSpotResponseDTO> createSpot(@RequestBody FishingSpotRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fishingSpotService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpot(@PathVariable Long id) {
        fishingSpotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}