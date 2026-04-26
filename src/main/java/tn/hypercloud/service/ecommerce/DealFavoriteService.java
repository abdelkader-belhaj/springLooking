package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.DealFavoriteDTO;
import tn.hypercloud.entity.ecommerce.Deal;
import tn.hypercloud.entity.ecommerce.DealFavorite;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.ecommerce.DealFavoriteRepository;
import tn.hypercloud.repository.ecommerce.DealRepository;
import tn.hypercloud.repository.user.UserRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DealFavoriteService {
    private final DealFavoriteRepository dealFavoriteRepository;
    private final UserRepository userRepository;
    private final DealRepository dealRepository;

    public DealFavoriteDTO createDealFavorite(DealFavoriteDTO dealFavoriteDTO) {
        User user = userRepository.findById(dealFavoriteDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + dealFavoriteDTO.getUserId()));
        Deal deal = dealRepository.findById(dealFavoriteDTO.getDealId())
                .orElseThrow(() -> new RuntimeException("Deal not found with id: " + dealFavoriteDTO.getDealId()));
        
        DealFavorite dealFavorite = DealFavorite.builder()
                .user(user)
                .deal(deal)
                .build();
        DealFavorite saved = dealFavoriteRepository.save(dealFavorite);
        // Note: favoritesCount is now calculated from DealFavorite table, no need to update
        return mapToDTO(saved);
    }

    public DealFavoriteDTO getDealFavoriteById(Long id) {
        DealFavorite dealFavorite = dealFavoriteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DealFavorite not found with id: " + id));
        return mapToDTO(dealFavorite);
    }

    public List<DealFavoriteDTO> getAllDealFavorites() {
        return dealFavoriteRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void deleteDealFavorite(Long id) {
        if (!dealFavoriteRepository.existsById(id)) {
            throw new RuntimeException("DealFavorite not found with id: " + id);
        }
        DealFavorite dealFavorite = dealFavoriteRepository.findById(id).get();
        // Note: favoritesCount is now calculated from DealFavorite table, no need to update
        dealFavoriteRepository.deleteById(id);
    }

    private DealFavoriteDTO mapToDTO(DealFavorite dealFavorite) {
        return DealFavoriteDTO.builder()
                .id(dealFavorite.getId())
                .userId(dealFavorite.getUser().getId())
                .dealId(dealFavorite.getDeal().getId())
                .addedAt(dealFavorite.getAddedAt())
                .build();
    }
}
