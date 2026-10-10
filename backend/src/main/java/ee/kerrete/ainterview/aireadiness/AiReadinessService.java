package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentRequest;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.ProfessionSummary;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Question;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiReadinessService {

    private final AiReadinessCatalog catalog;
    private final AiReadinessScorer scorer;
    private final AiReadinessCoach coach;

    public AiReadinessService(AiReadinessCatalog catalog, AiReadinessScorer scorer, AiReadinessCoach coach) {
        this.catalog = catalog;
        this.scorer = scorer;
        this.coach = coach;
    }

    public List<ProfessionSummary> professions() {
        return catalog.professions().stream()
                .map(p -> new ProfessionSummary(p.id(), p.name(), p.category()))
                .toList();
    }

    public List<Question> questions() {
        return catalog.questions();
    }

    public AssessmentResult assess(AssessmentRequest request) {
        Profession profession = catalog.profession(request.professionId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown profession '" + request.professionId() + "'"));
        AssessmentResult result = scorer.score(profession, request.answers());
        if (!request.personalize()) {
            return result;
        }
        return coach.personalNote(result, answerLabels(request.answers()))
                .map(note -> withNote(result, note))
                .orElse(result);
    }

    private Map<String, String> answerLabels(Map<String, String> answers) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (Question q : catalog.questions()) {
            String optionId = answers.get(q.id());
            q.options().stream()
                    .filter(o -> o.id().equals(optionId))
                    .findFirst()
                    .ifPresent(o -> labels.put(q.text(), o.label()));
        }
        return labels;
    }

    private static AssessmentResult withNote(AssessmentResult r, String note) {
        return new AssessmentResult(r.professionId(), r.professionName(), r.replaceabilityScore(),
                r.taskExposureScore(), r.personalAdjustment(), r.band(), r.headline(), r.readinessScore(),
                r.readinessLevel(), r.tasks(), r.humanEdge(), r.aiTools(), r.pivotRoles(), r.generalRoadmap(),
                r.professionRoadmap(), r.totalWeeks(), note);
    }
}
