package com.fishing.brazil.controller;

import com.fishing.brazil.dto.request.CatchRecordRequestDTO;
import com.fishing.brazil.dto.response.CatchRecordResponseDTO;
import com.fishing.brazil.service.CatchRecordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catch-records")
public class CatchRecordController {

    private final CatchRecordService catchRecordService;

    public CatchRecordController(CatchRecordService catchRecordService) {
        this.catchRecordService = catchRecordService;
    }

    @GetMapping
    public ResponseEntity<Page<CatchRecordResponseDTO>> getAllRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "catchDate") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(catchRecordService.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<CatchRecordResponseDTO> createRecord(@Valid @RequestBody CatchRecordRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catchRecordService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        catchRecordService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ranking/comprimento")
    public ResponseEntity<?> getRankingPorComprimento() {
        return ResponseEntity.ok(catchRecordService.getRankingPorComprimento());
    }

    @GetMapping("/ranking/peso")
    public ResponseEntity<?> getRankingPorPeso() {
        return ResponseEntity.ok(catchRecordService.getRankingPorPeso());
    }

    @GetMapping("/ranking/pescadores")
    public ResponseEntity<?> getRankingDePescadores() {
        return ResponseEntity.ok(catchRecordService.getRankingPescadores());
    }
}