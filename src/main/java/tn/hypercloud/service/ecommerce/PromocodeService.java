package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.PromocodeDTO;
import tn.hypercloud.entity.ecommerce.PromoCode;
import tn.hypercloud.repository.ecommerce.PromocodeRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromocodeService {
    private final PromocodeRepository promocodeRepository;

    public PromocodeDTO createPromocode(PromocodeDTO promocodeDTO) {
        PromoCode promoCode = PromoCode.builder()
                .code(promocodeDTO.getCode())
                .discountPercentage(promocodeDTO.getDiscountPercentage())
                .isActive(promocodeDTO.isActive())
                .build();
        PromoCode saved = promocodeRepository.save(promoCode);
        return mapToDTO(saved);
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
        PromoCode promoCode = promocodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PromoCode not found with id: " + id));
        
        promoCode.setCode(promocodeDTO.getCode());
        promoCode.setDiscountPercentage(promocodeDTO.getDiscountPercentage());
        promoCode.setActive(promocodeDTO.isActive());
        
        PromoCode updated = promocodeRepository.save(promoCode);
        return mapToDTO(updated);
    }

    public void deletePromocode(Long id) {
        if (!promocodeRepository.existsById(id)) {
            throw new RuntimeException("PromoCode not found with id: " + id);
        }
        promocodeRepository.deleteById(id);
    }

    private PromocodeDTO mapToDTO(PromoCode promoCode) {
        return PromocodeDTO.builder()
                .id(promoCode.getId())
                .code(promoCode.getCode())
                .discountPercentage(promoCode.getDiscountPercentage())
                .isActive(promoCode.isActive())
                .build();
    }
}
