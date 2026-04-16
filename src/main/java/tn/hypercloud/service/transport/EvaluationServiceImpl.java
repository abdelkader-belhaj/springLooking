package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.dto.transport.DriverReviewSummaryDto;
import tn.hypercloud.entity.transport.enums.EvaluationType;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Course;
import java.util.stream.Collectors;
import tn.hypercloud.entity.transport.EvaluationTransport;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.ChauffeurRepository;
import tn.hypercloud.repository.transport.CourseRepository;
import tn.hypercloud.repository.transport.EvaluationRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
@Service
@AllArgsConstructor
public class EvaluationServiceImpl implements IEvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ChauffeurRepository chauffeurRepository;
    @Override
    public EvaluationTransport addEvaluation(EvaluationTransport evaluationTransport) {
        // 1. Fetch the full Course
        if (evaluationTransport.getCourse() != null && evaluationTransport.getCourse().getIdCourse() != null) {
            Course course = courseRepository.findById(evaluationTransport.getCourse().getIdCourse())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            evaluationTransport.setCourse(course);
        } else {
            throw new IllegalArgumentException("Course ID is required");
        }

        // 2. Fetch the evaluateur User using the transient ID
        if (evaluationTransport.getEvaluateurId() != null) {
            User evaluateur = userRepository.findById(evaluationTransport.getEvaluateurId())
                    .orElseThrow(() -> new RuntimeException("Evaluateur not found with id: " + evaluationTransport.getEvaluateurId()));
            evaluationTransport.setEvaluateur(evaluateur);
        } else {
            throw new IllegalArgumentException("Evaluateur ID is required");
        }

        // 3. Fetch the evalue User using the transient ID
        if (evaluationTransport.getEvalueId() != null) {
            User evalue = userRepository.findById(evaluationTransport.getEvalueId())
                    .orElseThrow(() -> new RuntimeException("Evalue not found with id: " + evaluationTransport.getEvalueId()));
            evaluationTransport.setEvalue(evalue);
        } else {
            throw new IllegalArgumentException("Evalue ID is required");
        }

        EvaluationTransport existing = evaluationRepository.findFirstByCourseAndType(
                evaluationTransport.getCourse(),
                evaluationTransport.getType()
        );

        if (existing != null) {
            return enrich(existing);
        }

        return enrich(evaluationRepository.save(evaluationTransport));
    }
    @Override
    public EvaluationTransport updateEvaluation(EvaluationTransport evaluationTransport) {
        return enrich(evaluationRepository.save(evaluationTransport));
    }

    @Override
    public void deleteEvaluation(Long id) {
        evaluationRepository.deleteById(id);
    }

    @Override
    public EvaluationTransport getEvaluationById(Long id) {
        return evaluationRepository.findById(id).map(this::enrich).orElse(null);
    }

    @Override
    public List<EvaluationTransport> getAllEvaluations() {
        return evaluationRepository.findAll().stream().map(this::enrich).collect(Collectors.toList());
    }

    @Override
    public List<EvaluationTransport> getEvaluationsByCourse(Course course) {
        return evaluationRepository.findByCourse(course).stream().map(this::enrich).collect(Collectors.toList());
    }

    @Override
    public List<EvaluationTransport> getEvaluationsForUser(User user) {
        return evaluationRepository.findByEvalue(user).stream().map(this::enrich).collect(Collectors.toList());
    }

    @Override
    public List<EvaluationTransport> getClientReviewsForChauffeur(Long chauffeurId) {
        if (chauffeurId == null || chauffeurId <= 0) {
            return List.of();
        }

        return findClientToDriverEvaluations(chauffeurId).stream()
                .map(this::enrich)
                .sorted((left, right) -> {
                    if (left.getDateCreation() == null && right.getDateCreation() == null) {
                        return 0;
                    }
                    if (left.getDateCreation() == null) {
                        return 1;
                    }
                    if (right.getDateCreation() == null) {
                        return -1;
                    }
                    return right.getDateCreation().compareTo(left.getDateCreation());
                })
                .collect(Collectors.toList());
    }

    @Override
    public DriverReviewSummaryDto getDriverReviewSummary(Long chauffeurId) {
        if (chauffeurId == null || chauffeurId <= 0) {
            return DriverReviewSummaryDto.builder()
                    .success(false)
                    .chauffeurId(chauffeurId)
                    .message("Identifiant chauffeur invalide.")
                    .build();
        }

        List<EvaluationTransport> evaluations = findClientToDriverEvaluations(chauffeurId);

        if (evaluations.size() < 2) {
            return DriverReviewSummaryDto.builder()
                    .success(false)
                    .chauffeurId(chauffeurId)
                    .nombreAvis(evaluations.size())
                    .message("Il faut au moins 2 avis chauffeur pour generer un resume IA.")
                    .build();
        }

        List<String> comments = evaluations.stream()
                .map(EvaluationTransport::getCommentaire)
                .filter(comment -> comment != null && !comment.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());

        if (comments.isEmpty()) {
            return DriverReviewSummaryDto.builder()
                    .success(false)
                    .chauffeurId(chauffeurId)
                    .nombreAvis(evaluations.size())
                    .message("Aucun commentaire exploitable pour ce chauffeur.")
                    .build();
        }

        double averageNote = computeAverageNote(evaluations);
        List<String> highlights = extractTopicLabels(comments, true);
        List<String> concerns = extractTopicLabels(comments, false);

        String summary = composeSummary(
                averageNote,
                highlights.isEmpty() ? List.of("la qualite globale du service") : highlights,
                concerns.isEmpty() ? List.of("la climatisation ou les delais de depart") : concerns
        );

        return DriverReviewSummaryDto.builder()
                .success(true)
                .chauffeurId(chauffeurId)
                .summary(summary)
                .nombreAvis(evaluations.size())
                .averageNote(averageNote)
                .highlights(highlights)
                .concerns(concerns)
                .confidence(computeConfidence(evaluations.size(), averageNote))
                .build();
    }

    private List<EvaluationTransport> findClientToDriverEvaluations(Long chauffeurId) {
        List<EvaluationTransport> evaluations = evaluationRepository
                .findByTypeAndCourse_Chauffeur_IdChauffeur(EvaluationType.CLIENT_TO_DRIVER, chauffeurId);

        if (!evaluations.isEmpty()) {
            return evaluations;
        }

        // Fallback when rows are linked by evaluated user and not fully by course relation.
        Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId).orElse(null);
        Long driverUserId = chauffeur != null && chauffeur.getUtilisateur() != null
                ? chauffeur.getUtilisateur().getId()
                : null;

        if (driverUserId == null) {
            return List.of();
        }

        User driverUser = userRepository.findById(driverUserId).orElse(null);
        if (driverUser == null) {
            return List.of();
        }

        return evaluationRepository.findByEvalue(driverUser).stream()
                .filter(e -> e.getType() == EvaluationType.CLIENT_TO_DRIVER)
                .collect(Collectors.toList());
    }

    private double computeAverageNote(List<EvaluationTransport> evaluations) {
        double average = evaluations.stream()
                .map(EvaluationTransport::getNote)
                .filter(note -> note != null && note > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return Math.round(average * 10.0) / 10.0;
    }

    private String computeConfidence(int sourceCount, double averageNote) {
        if (sourceCount >= 8 && averageNote >= 4.3) {
            return "Fiabilite elevee";
        }
        if (sourceCount >= 4) {
            return "Fiabilite moyenne";
        }
        return "Fiabilite exploratoire";
    }

    private String composeSummary(double averageNote, List<String> highlights, List<String> concerns) {
        String positive = formatList(highlights, 2);
        String negative = formatList(concerns, 1);

        if (averageNote >= 4.6) {
            return "Les clients saluent surtout " + positive + 
                    ". Le chauffeur est tres bien evalue, avec seulement quelques retours ponctuels sur " +
                    negative + ".";
        }

        if (averageNote >= 4.1) {
            return "Les clients apprecient surtout " + positive +
                    ", tout en signalant parfois " + negative + ".";
        }

        return "Les avis sont plus mitiges: " + positive +
            " ressortent, mais " + negative + " revient regulierement.";
    }

    private String formatList(List<String> labels, int limit) {
        List<String> items = labels.stream()
                .filter(label -> label != null && !label.isBlank())
                .limit(limit)
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            return "la qualite globale";
        }

        if (items.size() == 1) {
            return items.get(0);
        }

        return items.get(0) + " et " + items.get(1);
    }

    private List<String> extractTopicLabels(List<String> comments, boolean positive) {
        Map<String, int[]> topicScores = buildTopicScores();

        for (String comment : comments) {
            String normalized = normalize(comment);
            for (Map.Entry<String, int[]> entry : topicScores.entrySet()) {
                int[] score = entry.getValue();
                List<String> positiveWords = positiveKeywords(entry.getKey());
                List<String> negativeWords = negativeKeywords(entry.getKey());

                if (positiveWords.stream().anyMatch(normalized::contains)) {
                    score[0]++;
                }
                if (negativeWords.stream().anyMatch(normalized::contains)) {
                    score[1]++;
                }
            }
        }

        Comparator<Map.Entry<String, int[]>> comparator = positive
                ? Comparator.comparingInt((Map.Entry<String, int[]> e) -> (e.getValue()[0] - e.getValue()[1]))
                    .thenComparingInt(e -> e.getValue()[0])
                : Comparator.comparingInt((Map.Entry<String, int[]> e) -> (e.getValue()[1] - e.getValue()[0]))
                    .thenComparingInt(e -> e.getValue()[1]);

        return topicScores.entrySet().stream()
                .filter(entry -> positive
                        ? entry.getValue()[0] > entry.getValue()[1]
                        : entry.getValue()[1] > entry.getValue()[0])
                .sorted(comparator.reversed())
                .map(Map.Entry::getKey)
                .limit(3)
                .collect(Collectors.toList());
    }

    private Map<String, int[]> buildTopicScores() {
        Map<String, int[]> map = new LinkedHashMap<>();
        map.put("la proprete du vehicule", new int[]{0, 0});
        map.put("la ponctualite", new int[]{0, 0});
        map.put("la conduite", new int[]{0, 0});
        map.put("la climatisation", new int[]{0, 0});
        map.put("l'accueil du chauffeur", new int[]{0, 0});
        map.put("le rapport qualite-prix", new int[]{0, 0});
        return map;
    }

    private List<String> positiveKeywords(String topicLabel) {
        return switch (topicLabel) {
            case "la proprete du vehicule" -> List.of("propre", "impeccable", "net", "nettoye");
            case "la ponctualite" -> List.of("ponctuel", "a l heure", "rapide");
            case "la conduite" -> List.of("conduite souple", "prudence", "calme", "serein");
            case "la climatisation" -> List.of("clim", "air frais", "frais");
            case "l'accueil du chauffeur" -> List.of("accueillant", "sympa", "professionnel", "aimable");
            case "le rapport qualite-prix" -> List.of("bon rapport", "raisonnable", "prix correct");
            default -> new ArrayList<>();
        };
    }

    private List<String> negativeKeywords(String topicLabel) {
        return switch (topicLabel) {
            case "la proprete du vehicule" -> List.of("sale", "odeur", "salete");
            case "la ponctualite" -> List.of("retard", "attente", "en retard");
            case "la conduite" -> List.of("agressif", "brutal", "freinage");
            case "la climatisation" -> List.of("clim faible", "chaud", "trop chaud");
            case "l'accueil du chauffeur" -> List.of("impoli", "froid", "desagreable");
            case "le rapport qualite-prix" -> List.of("cher", "trop cher");
            default -> new ArrayList<>();
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("ù", "u")
                .replace("ç", "c")
                .trim();
    }

    private EvaluationTransport enrich(EvaluationTransport evaluation) {
        if (evaluation == null) {
            return null;
        }

        evaluation.setCourseId(evaluation.getCourse() != null ? evaluation.getCourse().getIdCourse() : null);
        evaluation.setEvaluateurId(evaluation.getEvaluateur() != null ? evaluation.getEvaluateur().getId() : null);
        evaluation.setEvalueId(evaluation.getEvalue() != null ? evaluation.getEvalue().getId() : null);
        evaluation.setEvaluateurNom(evaluation.getEvaluateur() != null ? evaluation.getEvaluateur().getUsername() : null);
        evaluation.setEvalueNom(evaluation.getEvalue() != null ? evaluation.getEvalue().getUsername() : null);
        return evaluation;
    }
}