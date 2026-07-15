package com.fishing.brazil.controller;

import com.fishing.brazil.dto.request.BaitRequestDTO;
import com.fishing.brazil.dto.response.BaitResponseDTO;
import com.fishing.brazil.service.BaitService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/baits")
public class BaitController {

    private final BaitService baitService;

    public BaitController(BaitService baitService) {
        this.baitService = baitService;
    }

    @GetMapping
    public ResponseEntity<Page<BaitResponseDTO>> getBaits(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(baitService.findBaits(name, pageable));
    }

    @PostMapping
    public ResponseEntity<BaitResponseDTO> createBait(@RequestBody BaitRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(baitService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBait(@PathVariable Long id) {
        baitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}