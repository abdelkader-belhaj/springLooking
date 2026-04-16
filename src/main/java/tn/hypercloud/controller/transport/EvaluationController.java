package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.transport.DriverReviewSummaryDto;
import tn.hypercloud.entity.transport.EvaluationTransport;
import tn.hypercloud.service.transport.*;

import java.util.List;
@RestController
@RequestMapping("/hypercloud/evaluations")
@AllArgsConstructor
public class EvaluationController {

    private final IEvaluationService evaluationService;

    @PostMapping
    public EvaluationTransport addEvaluation(@RequestBody EvaluationTransport evaluationTransport) {
        return evaluationService.addEvaluation(evaluationTransport);
    }

    @PutMapping("/{id}")
    public EvaluationTransport updateEvaluation(@PathVariable Long id, @RequestBody EvaluationTransport evaluationTransport) {
        evaluationTransport.setIdEvaluation(id);
        return evaluationService.updateEvaluation(evaluationTransport);
    }

    @DeleteMapping("/{id}")
    public void deleteEvaluation(@PathVariable Long id) {
        evaluationService.deleteEvaluation(id);
    }

    @GetMapping("/{id}")
    public EvaluationTransport getEvaluationById(@PathVariable Long id) {
        return evaluationService.getEvaluationById(id);
    }

    @GetMapping
    public List<EvaluationTransport> getAllEvaluations() {
        return evaluationService.getAllEvaluations();
    }

    @GetMapping("/chauffeur/{chauffeurId}/summary-ai")
    public DriverReviewSummaryDto getDriverSummary(@PathVariable Long chauffeurId) {
        return evaluationService.getDriverReviewSummary(chauffeurId);
    }

    @GetMapping("/chauffeur/{chauffeurId}/client-reviews")
    public List<EvaluationTransport> getDriverClientReviews(@PathVariable Long chauffeurId) {
        return evaluationService.getClientReviewsForChauffeur(chauffeurId);
    }
}