package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.PromocodeDTO;
import tn.hypercloud.service.ecommerce.PromocodeService;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/promocodes")
@RequiredArgsConstructor
public class PromocodeController {
    private final PromocodeService promocodeService;

    @PostMapping
    public ResponseEntity<PromocodeDTO> createPromocode(@RequestBody PromocodeDTO promocodeDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promocodeService.createPromocode(promocodeDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromocodeDTO> getPromocodeById(@PathVariable Long id) {
        return ResponseEntity.ok(promocodeService.getPromocodeById(id));
    }

    @GetMapping
    public ResponseEntity<List<PromocodeDTO>> getAllPromocodes() {
        return ResponseEntity.ok(promocodeService.getAllPromocodes());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromocodeDTO> updatePromocode(@PathVariable Long id, @RequestBody PromocodeDTO promocodeDTO) {
        return ResponseEntity.ok(promocodeService.updatePromocode(id, promocodeDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromocode(@PathVariable Long id) {
        promocodeService.deletePromocode(id);
        return ResponseEntity.noContent().build();
    }
}
