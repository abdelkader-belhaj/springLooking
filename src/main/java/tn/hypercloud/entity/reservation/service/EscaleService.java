package tn.hypercloud.entity.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.reservation.Escale;
import tn.hypercloud.entity.reservation.Vol;
import tn.hypercloud.entity.reservation.dto.EscaleRequest;
import tn.hypercloud.entity.reservation.dto.EscaleResponse;
import tn.hypercloud.repository.reservation.EscaleRepository;
import tn.hypercloud.repository.reservation.VolRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EscaleService {

    private final EscaleRepository escaleRepository;
    private final VolRepository volRepository;

    public List<EscaleResponse> getEscalesByVol(Integer volId) {
        return escaleRepository.findByVolId(volId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EscaleResponse addEscale(Integer volId, EscaleRequest request) {
        Vol vol = volRepository.findById(volId)
                .orElseThrow(() -> new RuntimeException("Vol non trouvé"));
        
        Escale escale = Escale.builder()
                .ville(request.getVille())
                .duree(request.getDuree())
                .vol(vol)
                .build();
        
        return mapToResponse(escaleRepository.save(escale));
    }

    @Transactional
    public void deleteEscale(Integer id) {
        escaleRepository.deleteById(id);
    }

    private EscaleResponse mapToResponse(Escale escale) {
        EscaleResponse response = new EscaleResponse();
        response.setId(escale.getId());
        response.setVille(escale.getVille());
        response.setDuree(escale.getDuree());
        return response;
    }
}
