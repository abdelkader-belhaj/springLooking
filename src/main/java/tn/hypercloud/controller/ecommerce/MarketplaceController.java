package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.payload.response.ApiResponse;
import tn.hypercloud.payload.response.UserResponse;
import tn.hypercloud.service.UserService;
import tn.hypercloud.entity.user.Role;
import java.util.List;

/**
 * Admin endpoints for marketplace management
 */
@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {
    private final UserService userService;

    /**
     * Get all artisans (users with VENDEUR_ARTI role)
     * GET /api/marketplace/artisans
     */
    @GetMapping("/artisans")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllArtisans() {
        List<UserResponse> artisans = userService.getUsersByRole(Role.VENDEUR_ARTI);
        return ResponseEntity.ok(
                ApiResponse.success("Artisans retrieved", artisans)
        );
    }
}
