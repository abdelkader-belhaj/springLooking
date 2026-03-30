package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.Matching;
import tn.hypercloud.service.transport.*;

import java.util.List;
@RestController
@RequestMapping("/hypercloud/matchings")
@AllArgsConstructor
public class MatchingController {

    private final IMatchingService matchingService;

    @PostMapping
    public Matching addMatching(@RequestBody Matching matching) {
        return matchingService.addMatching(matching);
    }

    @PutMapping("/{id}")
    public Matching updateMatching(@PathVariable Long id, @RequestBody Matching matching) {
        matching.setIdMatching(id);
        return matchingService.updateMatching(matching);
    }

    @DeleteMapping("/{id}")
    public void deleteMatching(@PathVariable Long id) {
        matchingService.deleteMatching(id);
    }

    @GetMapping("/{id}")
    public Matching getMatchingById(@PathVariable Long id) {
        return matchingService.getMatchingById(id);
    }

    @GetMapping
    public List<Matching> getAllMatchings() {
        return matchingService.getAllMatchings();
    }

    /*@PutMapping("/{id}/accepter")
    public Matching acceptMatching(@PathVariable Long id) {
        return matchingService.acceptMatching(id);
    }
*/
    @PutMapping("/{id}/accepter")
    public Course acceptMatching(@PathVariable Long id) {
        return matchingService.acceptMatching(id).getCourse();
    }
    @PutMapping("/{id}/rejeter")
    public Matching rejectMatching(@PathVariable Long id) {
        return matchingService.rejectMatching(id);
    }
}