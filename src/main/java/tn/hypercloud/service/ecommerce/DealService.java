package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.DealDTO;
import tn.hypercloud.entity.ecommerce.Deal;
import tn.hypercloud.repository.ecommerce.DealRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DealService {
    private final DealRepository dealRepository;

    public DealDTO createDeal(DealDTO dealDTO) {
        Deal deal = Deal.builder()
                .title(dealDTO.getTitle())
                .description(dealDTO.getDescription())
                .location(dealDTO.getLocation())
                .region(Deal.Region.valueOf(dealDTO.getRegion()))
                .budget(Deal.Budget.valueOf(dealDTO.getBudget()))
                .image(dealDTO.getImage())
                .activityType(Deal.ActivityType.valueOf(dealDTO.getActivityType()))
                .environment(Deal.Environment.valueOf(dealDTO.getEnvironment()))
                .category(Deal.Category.valueOf(dealDTO.getCategory()))
                .duration(Deal.Duration.valueOf(dealDTO.getDuration()))
                .build();
        Deal saved = dealRepository.save(deal);
        return mapToDTO(saved);
    }

    public DealDTO getDealById(Long id) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deal not found with id: " + id));
        return mapToDTO(deal);
    }

    public List<DealDTO> getAllDeals() {
        return dealRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public DealDTO updateDeal(Long id, DealDTO dealDTO) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deal not found with id: " + id));
        
        deal.setTitle(dealDTO.getTitle());
        deal.setDescription(dealDTO.getDescription());
        deal.setLocation(dealDTO.getLocation());
        deal.setRegion(Deal.Region.valueOf(dealDTO.getRegion()));
        deal.setBudget(Deal.Budget.valueOf(dealDTO.getBudget()));
        deal.setImage(dealDTO.getImage());
        deal.setActivityType(Deal.ActivityType.valueOf(dealDTO.getActivityType()));
        deal.setEnvironment(Deal.Environment.valueOf(dealDTO.getEnvironment()));
        deal.setCategory(Deal.Category.valueOf(dealDTO.getCategory()));
        deal.setDuration(Deal.Duration.valueOf(dealDTO.getDuration()));
        
        Deal updated = dealRepository.save(deal);
        return mapToDTO(updated);
    }

    public void deleteDeal(Long id) {
        if (!dealRepository.existsById(id)) {
            throw new RuntimeException("Deal not found with id: " + id);
        }
        dealRepository.deleteById(id);
    }

    private DealDTO mapToDTO(Deal deal) {
        return DealDTO.builder()
                .id(deal.getId())
                .title(deal.getTitle())
                .description(deal.getDescription())
                .location(deal.getLocation())
                .region(deal.getRegion().toString())
                .budget(deal.getBudget().toString())
                .image(deal.getImage())
                .activityType(deal.getActivityType().toString())
                .environment(deal.getEnvironment().toString())
                .category(deal.getCategory().toString())
                .duration(deal.getDuration().toString())
                .favoritesCount(deal.getFavoritesCount())
                .build();
    }
}
