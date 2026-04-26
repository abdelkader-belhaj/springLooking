package tn.hypercloud.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.accommodation.UserPreferences;
import tn.hypercloud.payload.request.UserPreferencesRequest;
import tn.hypercloud.payload.response.UserPreferencesResponse;
import tn.hypercloud.repository.accommodation.UserPreferencesRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository prefsRepo;

    public UserPreferencesResponse saveOrUpdate(Integer userId, UserPreferencesRequest req) {
        UserPreferences prefs = prefsRepo.findByUserId(userId)
                .orElse(UserPreferences.builder().userId(userId).build());

        prefs.setBudgetMax(req.getBudgetMax());
        prefs.setTypeSejour(req.getTypeSejour());
        prefs.setVillePreferee(req.getVillePreferee());
        prefs.setCapaciteMin(req.getCapaciteMin());
        prefs.setEquipementsSouhaites(req.getEquipementsSouhaites());
        prefs.setAmbiance(req.getAmbiance());

        UserPreferences saved = prefsRepo.save(prefs);
        return toResponse(saved);
    }

    public Optional<UserPreferencesResponse> getByUserId(Integer userId) {
        return prefsRepo.findByUserId(userId).map(this::toResponse);
    }

    private UserPreferencesResponse toResponse(UserPreferences p) {
        return UserPreferencesResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .budgetMax(p.getBudgetMax())
                .typeSejour(p.getTypeSejour())
                .villePreferee(p.getVillePreferee())
                .capaciteMin(p.getCapaciteMin())
                .equipementsSouhaites(p.getEquipementsSouhaites())
                .ambiance(p.getAmbiance())
                .build();
    }
}
