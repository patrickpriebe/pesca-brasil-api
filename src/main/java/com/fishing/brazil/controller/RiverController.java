package com.fishing.brazil.controller;

import com.fishing.brazil.dto.request.RiverRequestDTO;
import com.fishing.brazil.dto.response.RiverResponseDTO;
import com.fishing.brazil.service.RiverService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rivers")
public class RiverController {

    private final RiverService riverService;

    public RiverController(RiverService riverService) {
        this.riverService = riverService;
    }

    @GetMapping
    public ResponseEntity<Page<RiverResponseDTO>> getRivers(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return ResponseEntity.ok(riverService.findRivers(name, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiverResponseDTO> getRiverById(@PathVariable Long id) {
        return riverService.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RiverResponseDTO> createRiver(@RequestBody RiverRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riverService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRiver(@PathVariable Long id) {
        riverService.delete(id);
        return ResponseEntity.noContent().build();
    }
}