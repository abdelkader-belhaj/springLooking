package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.DealDTO;
import tn.hypercloud.service.ecommerce.DealService;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/deals")
@RequiredArgsConstructor
public class DealController {
    private final DealService dealService;

    @PostMapping
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
    public ResponseEntity<DealDTO> updateDeal(@PathVariable Long id, @RequestBody DealDTO dealDTO) {
        return ResponseEntity.ok(dealService.updateDeal(id, dealDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeal(@PathVariable Long id) {
        dealService.deleteDeal(id);
        return ResponseEntity.noContent().build();
    }
}
