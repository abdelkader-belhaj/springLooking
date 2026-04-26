package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import tn.hypercloud.dto.ecommerce.DealDTO;
import tn.hypercloud.service.ecommerce.DealService;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/deals")
@RequiredArgsConstructor
public class DealController {
    private final DealService dealService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DealDTO> createDeal(@RequestBody DealDTO dealDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealService.createDeal(dealDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DealDTO> getDealById(@PathVariable Long id) {
        return ResponseEntity.ok(dealService.getDealById(id));
    }

    @GetMapping
    public ResponseEntity<List<DealDTO>> getAllDeals() {
        return ResponseEntity.ok(dealService.getAllDeals());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DealDTO> updateDeal(@PathVariable Long id, @RequestBody DealDTO dealDTO) {
        return ResponseEntity.ok(dealService.updateDeal(id, dealDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDeal(@PathVariable Long id) {
        dealService.deleteDeal(id);
        return ResponseEntity.noContent().build();
    }
   
    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<DealDTO> uploadDealImage(
        @PathVariable Long id,
        @RequestParam("image") MultipartFile file) {
    
    try {
        String uploadDir = "uploads/deals/";
        Files.createDirectories(Paths.get(uploadDir));
        
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + filename);
        Files.write(filePath, file.getBytes());
        
        DealDTO updated = dealService.updateDealImage(id, uploadDir + filename);
        return ResponseEntity.ok(updated);
    } catch (IOException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

    @PostMapping("/admin/recalculate-favorites")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> recalculateFavoritesCounts() {
        dealService.recalculateAllFavoritesCounts();
        return ResponseEntity.ok("All favorite counts recalculated successfully");
    }
}
