package tn.hypercloud.entity.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.reservation.dto.EscaleRequest;
import tn.hypercloud.entity.reservation.dto.EscaleResponse;
import tn.hypercloud.entity.reservation.service.EscaleService;

import java.util.List;

@RestController
@RequestMapping("/api/escales")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EscaleController {

    private final EscaleService escaleService;

    @GetMapping("/vol/{volId}")
    public ResponseEntity<List<EscaleResponse>> getByVol(@PathVariable Integer volId) {
        return ResponseEntity.ok(escaleService.getEscalesByVol(volId));
    }

    @PostMapping("/vol/{volId}")
    public ResponseEntity<EscaleResponse> add(@PathVariable Integer volId, @RequestBody EscaleRequest request) {
        return ResponseEntity.ok(escaleService.addEscale(volId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        escaleService.deleteEscale(id);
        return ResponseEntity.noContent().build();
    }
}
