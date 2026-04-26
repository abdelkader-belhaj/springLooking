package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.DealFavoriteDTO;
import tn.hypercloud.dto.ecommerce.DealDTO;
import tn.hypercloud.payload.response.ApiResponse;
import tn.hypercloud.service.ecommerce.DealFavoriteService;
import tn.hypercloud.service.ecommerce.DealService;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.entity.ecommerce.DealFavorite;
import tn.hypercloud.repository.user.UserRepository;
import tn.hypercloud.repository.ecommerce.DealFavoriteRepository;
import java.util.List;

@RestController
@RequestMapping("/api/ecommerce/deal-favorites")
@RequiredArgsConstructor
public class DealFavoriteController {
    private final DealFavoriteService dealFavoriteService;
    private final UserRepository userRepository;
    private final DealFavoriteRepository dealFavoriteRepository;
    private final DealService dealService;

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

    @PostMapping("/toggle/{dealId}")
public ResponseEntity<ApiResponse<Boolean>> toggleFavorite(@PathVariable Long dealId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    boolean isFavorite = dealFavoriteRepository.existsByUser_IdAndDeal_Id(user.getId(), dealId);

    if (isFavorite) {
        // ✅ use service method so the count gets decremented
        DealFavorite existing = dealFavoriteRepository
                .findByUser_IdAndDeal_Id(user.getId(), dealId)
                .orElseThrow(() -> new RuntimeException("Favorite not found"));
        dealFavoriteService.deleteDealFavorite(existing.getId());
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites", false));
    } else {
        DealFavoriteDTO dto = new DealFavoriteDTO();
        dto.setUserId(user.getId());
        dto.setDealId(dealId);
        dealFavoriteService.createDealFavorite(dto);
        return ResponseEntity.ok(ApiResponse.success("Added to favorites", true));
    }
}

    @GetMapping("/my-favorites")
    public ResponseEntity<ApiResponse<List<DealDTO>>> getMyFavorites() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<DealFavorite> favorites = dealFavoriteRepository.findByUser_Id(user.getId());
        List<DealDTO> deals = favorites.stream()
                .map(f -> dealService.getDealById(f.getDeal().getId()))
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Favorites retrieved", deals));
    }
}