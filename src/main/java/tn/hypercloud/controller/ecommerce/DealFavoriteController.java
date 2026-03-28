package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.DealFavoriteDTO;
import tn.hypercloud.service.ecommerce.DealFavoriteService;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/deal-favorites")
@RequiredArgsConstructor
public class DealFavoriteController {
    private final DealFavoriteService dealFavoriteService;

    @PostMapping
    public ResponseEntity<DealFavoriteDTO> createDealFavorite(@RequestBody DealFavoriteDTO dealFavoriteDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealFavoriteService.createDealFavorite(dealFavoriteDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DealFavoriteDTO> getDealFavoriteById(@PathVariable Long id) {
        return ResponseEntity.ok(dealFavoriteService.getDealFavoriteById(id));
    }

    @GetMapping
    public ResponseEntity<List<DealFavoriteDTO>> getAllDealFavorites() {
        return ResponseEntity.ok(dealFavoriteService.getAllDealFavorites());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDealFavorite(@PathVariable Long id) {
        dealFavoriteService.deleteDealFavorite(id);
        return ResponseEntity.noContent().build();
    }
}
