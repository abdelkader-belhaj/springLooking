package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.PromocodeDTO;
import tn.hypercloud.entity.ecommerce.PromoCode;
import tn.hypercloud.repository.ecommerce.PromoCodeRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromocodeService {
    private final PromoCodeRepository promocodeRepository;
    private final PromoEmailService promoEmailService;

    /**
     * Generate a unique promo code automatically
     */
    public PromocodeDTO generatePromocode(PromocodeDTO promocodeDTO) {
        // Generate a unique code if not provided
        String code = promocodeDTO.getCode();
        if (code == null || code.isEmpty()) {
            code = generateUniqueCode();
        }
        
        PromoCode promoCode = PromoCode.builder()
                .code(code)
                .discountPercentage(promocodeDTO.getDiscountPercentage() > 0 ? 
                    promocodeDTO.getDiscountPercentage() : 10) // Default 10%
                .isActive(true)
                .timesUsed(0)
                .maxUses(promocodeDTO.getMaxUses())
                .build();
        
        PromoCode saved = promocodeRepository.save(promoCode);
        return mapToDTO(saved);
    }

    /**
     * Generate a unique code if not provided in request
     */
    private String generateUniqueCode() {
        String code;
        boolean exists;
        do {
            code = "PROMO" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8).toUpperCase();
            exists = promocodeRepository.existsByCode(code);
        } while (exists);
        return code;
    }

    public PromocodeDTO createPromocode(PromocodeDTO promocodeDTO) {
        return generatePromocode(promocodeDTO);
    }

    public PromocodeDTO getPromocodeById(Long id) {
        PromoCode promoCode = promocodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PromoCode not found with id: " + id));
        return mapToDTO(promoCode);
    }

    public List<PromocodeDTO> getAllPromocodes() {
        return promocodeRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public PromocodeDTO updatePromocode(Long id, PromocodeDTO promocodeDTO) {
        System.out.println("🔵 [Service] updatePromocode called with id: " + id);
        System.out.println("🔵 [Service] DTO received: " + promocodeDTO);
        System.out.println("🔵 [Service] DTO.isActive(): " + promocodeDTO.getIsActive());
        
        PromoCode promoCode = promocodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PromoCode not found with id: " + id));
        
        System.out.println("🔵 [Service] Found PromoCode - Before update:");
        System.out.println("    - isActive: " + promoCode.getIsActive());
        System.out.println("    - discount: " + promoCode.getDiscountPercentage());
        
        promoCode.setDiscountPercentage(promocodeDTO.getDiscountPercentage());
        promoCode.setIsActive(promocodeDTO.getIsActive());
        if (promocodeDTO.getMaxUses() != null) {
            promoCode.setMaxUses(promocodeDTO.getMaxUses());
        }
        
        System.out.println("🔵 [Service] PromoCode - After setting values:");
        System.out.println("    - isActive: " + promoCode.getIsActive());
        System.out.println("    - discount: " + promoCode.getDiscountPercentage());
        
        PromoCode updated = promocodeRepository.save(promoCode);
        System.out.println("✅ [Service] Saved to DB - isActive is now: " + updated.getIsActive());
        
        PromocodeDTO result = mapToDTO(updated);
        System.out.println("✅ [Service] Mapped to DTO - isActive: " + result.getIsActive());
        
        return result;
    }

    public void deletePromocode(Long id) {
        if (!promocodeRepository.existsById(id)) {
            throw new RuntimeException("PromoCode not found with id: " + id);
        }
        promocodeRepository.deleteById(id);
    }

    /**
     * Use a promo code - increment usage count and deactivate if max uses reached
     */
    public void usePromoCode(Long promoCodeId) {
        PromoCode promoCode = promocodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new RuntimeException("PromoCode not found with id: " + promoCodeId));
        
        promoCode.setTimesUsed(promoCode.getTimesUsed() + 1);
        
        // Auto-deactivate if max uses reached
        if (promoCode.getMaxUses() != null && 
            promoCode.getTimesUsed() >= promoCode.getMaxUses()) {
            promoCode.setIsActive(false);
        }
        
        promocodeRepository.save(promoCode);
    }

    /**
     * Send promo code to email and activate if needed
     */
    public PromocodeDTO sendPromoCodeToEmail(Long id, String email) {
        PromoCode promoCode = promocodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PromoCode not found with id: " + id));

        // Activate if not already active
        if (!promoCode.getIsActive()) {
            promoCode.setIsActive(true);
            promoCode = promocodeRepository.save(promoCode);
        }

        // Send email
        promoEmailService.sendPromoCodeEmail(email, promoCode);

        return mapToDTO(promoCode);
    }

    private PromocodeDTO mapToDTO(PromoCode promoCode) {
        System.out.println("🔵 [mapToDTO] Converting PromoCode to DTO");
        System.out.println("    - ID: " + promoCode.getId());
        System.out.println("    - isActive from entity: " + promoCode.getIsActive());
        System.out.println("    - isActive method: " + promoCode.getIsActive());
        
        PromocodeDTO dto = PromocodeDTO.builder()
                .id(promoCode.getId())
                .code(promoCode.getCode())
                .discountPercentage(promoCode.getDiscountPercentage())
                .isActive(promoCode.getIsActive())
                .timesUsed(promoCode.getTimesUsed())
                .maxUses(promoCode.getMaxUses())
                .createdAt(promoCode.getCreatedAt())
                .build();
        
        System.out.println("✅ [mapToDTO] DTO created with isActive: " + dto.getIsActive());
        return dto;
    }
}
