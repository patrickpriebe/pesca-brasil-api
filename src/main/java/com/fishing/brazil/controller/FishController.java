package com.fishing.brazil.controller;

import com.fishing.brazil.dto.request.FishRequestDTO;
import com.fishing.brazil.dto.response.FishResponseDTO;
import com.fishing.brazil.service.FishService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fishes")
public class FishController {

    private final FishService fishService;

    public FishController(FishService fishService) {
        this.fishService = fishService;
    }

    @GetMapping
    public ResponseEntity<Page<FishResponseDTO>> getFishes(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "commonName") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(fishService.findFishes(name, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FishResponseDTO> getFishById(@PathVariable Long id) {
        return fishService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FishResponseDTO> createFish(@Valid @RequestBody FishRequestDTO dto) {
        FishResponseDTO savedFish = fishService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFish);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFish(@PathVariable Long id) {
        fishService.delete(id);
        return ResponseEntity.noContent().build();
    }
}