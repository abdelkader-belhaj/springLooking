package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.PromocodeDTO;
import tn.hypercloud.dto.ecommerce.SendPromoCodeRequest;
import tn.hypercloud.entity.ecommerce.PromoCode;
import tn.hypercloud.service.ecommerce.PromocodeService;
import tn.hypercloud.repository.ecommerce.PromoCodeRepository;
import tn.hypercloud.payload.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Map;



/**
 * Admin endpoints for managing promo codes via marketplace
 */
@RestController
@RequestMapping("/api/promo-codes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromoCodeController {
    private final PromocodeService promocodeService;
    private final PromoCodeRepository promocodeRepository;

    /**
     * Generate a new promo code with default 10% discount (ADMIN ONLY)
     * POST /api/promo-codes
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PromocodeDTO>> generatePromoCode(@RequestBody(required = false) PromocodeDTO dto) {
        PromocodeDTO newCode = promocodeService.generatePromocode(
            dto != null ? dto : PromocodeDTO.builder().build()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promo code generated", newCode));
    }

    /**
     * Get all promo codes
     * GET /api/promo-codes
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromocodeDTO>>> getAllPromoCodes() {
        List<PromocodeDTO> codes = promocodeService.getAllPromocodes();
        return ResponseEntity.ok(ApiResponse.success("Promo codes retrieved", codes));
    }

    /**
     * Get a specific promo code by ID
     * GET /api/promo-codes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromocodeDTO>> getPromoCodeById(@PathVariable Long id) {
        PromocodeDTO code = promocodeService.getPromocodeById(id);
        return ResponseEntity.ok(ApiResponse.success("Promo code retrieved", code));
    }

    /**
     * Update a promo code (discount %, max uses, active status)
     * PUT /api/promo-codes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromocodeDTO>> updatePromoCode(
            @PathVariable Long id,
            @RequestBody PromocodeDTO dto) {
        System.out.println("🔵 [Controller] PUT /api/promo-codes/" + id);
        System.out.println("🔵 [Controller] Received DTO: " + dto);
        System.out.println("🔵 [Controller] isActive from DTO: " + dto.getIsActive());
        PromocodeDTO updated = promocodeService.updatePromocode(id, dto);
        System.out.println("✅ [Controller] Updated promo code: " + updated);
        return ResponseEntity.ok(ApiResponse.success("Promo code updated", updated));
    }

    /**
     * Toggle promo code active status
     * PATCH /api/promo-codes/{id}
     */
    @PatchMapping("/{id}")
public ResponseEntity<ApiResponse<PromocodeDTO>> togglePromoCode(
        @PathVariable Long id,
        @RequestBody Map<String, Object> payload) {
    
    PromocodeDTO existing = promocodeService.getPromocodeById(id);
    
    if (payload.containsKey("isActive")) {
        existing.setIsActive((Boolean) payload.get("isActive"));
    }
    
    PromocodeDTO updated = promocodeService.updatePromocode(id, existing);
    return ResponseEntity.ok(ApiResponse.success("Promo code status updated", updated));
}

    /**
     * Send promo code to email
     * POST /api/promo-codes/{id}/send
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<PromocodeDTO>> sendPromoCode(
            @PathVariable Long id,
            @RequestBody SendPromoCodeRequest request) {
        PromocodeDTO updated = promocodeService.sendPromoCodeToEmail(id, request.email());
        return ResponseEntity.ok(ApiResponse.success("Promo code sent successfully", updated));
    }

    /**
     * Delete a promo code
     * DELETE /api/promo-codes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePromoCode(@PathVariable Long id) {
        promocodeService.deletePromocode(id);
        return ResponseEntity.ok(ApiResponse.success("Promo code deleted", null));
    }
    @GetMapping("/validate/{code}")
public ResponseEntity<ApiResponse<PromocodeDTO>> validatePromoCode(@PathVariable String code) {
    try {
        System.out.println("🔵 Validating promo code: " + code);
        PromoCode promo = promocodeRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Promo code not found: " + code));
        System.out.println("✅ Found promo code: " + promo.getId());
        PromocodeDTO dto = promocodeService.getPromocodeById(promo.getId());
        System.out.println("✅ DTO built: " + dto.getCode());
        return ResponseEntity.ok(ApiResponse.success("Valid", dto));
    } catch (Exception e) {
        System.out.println("❌ Error validating promo code: " + e.getMessage());
        e.printStackTrace();
        throw e;
    }
}




}

