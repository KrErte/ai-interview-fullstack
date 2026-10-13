package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.GeneralStageResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.RiskBand;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.StageStatus;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.TaskExposure;
import ee.kerrete.ainterview.aireadiness.CatalogModels.GeneralStage;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.aireadiness.CatalogModels.ProfessionTask;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Question;
import ee.kerrete.ainterview.aireadiness.CatalogModels.QuestionOption;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Deterministlik ja seletatav skoorimine.
 * <p>
 * Asendatavus = ameti ülesannete AI-eksponeeritus (Σ osakaal × automatiseeritavus)
 * + isiklik korrektsioon küsimustiku vastustest (piiratud ±30 punktiga).
 * AI valmisolek = AI kasutuse ja õppimise küsimuste summa (0..100).
 */
@Component
public class AiReadinessScorer {

    static final int MAX_ADJUSTMENT = 30;
    static final int DEFAULT_WEEKLY_HOURS = 3;

    private final AiReadinessCatalog catalog;

    public AiReadinessScorer(AiReadinessCatalog catalog) {
        this.catalog = catalog;
    }

    public AssessmentResult score(Profession profession, Map<String, String> answers) {
        int exposure = (int) Math.round(profession.tasks().stream()
                .mapToDouble(t -> t.share() * t.automation())
                .sum());

        int riskDelta = 0;
        int readiness = 0;
        int weeklyHours = DEFAULT_WEEKLY_HOURS;
        for (Question q : catalog.questions()) {
            String optionId = answers.get(q.id());
            if (optionId == null) {
                continue;
            }
            QuestionOption option = q.options().stream()
                    .filter(o -> o.id().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown option '" + optionId + "' for question '" + q.id() + "'"));
            riskDelta += option.risk();
            readiness += option.readiness();
            if (option.weeklyHours() != null) {
                weeklyHours = option.weeklyHours();
            }
        }
        for (String questionId : answers.keySet()) {
            if (catalog.questions().stream().noneMatch(q -> q.id().equals(questionId))) {
                throw new IllegalArgumentException("Unknown question '" + questionId + "'");
            }
        }

        int adjustment = clamp(riskDelta, -MAX_ADJUSTMENT, MAX_ADJUSTMENT);
        int replaceability = clamp(exposure + adjustment, 3, 97);
        int readinessScore = clamp(readiness, 0, 100);
        RiskBand band = band(replaceability);

        List<GeneralStageResult> general = generalRoadmap(readinessScore, weeklyHours);
        int remainingWeeks = general.stream()
                .filter(s -> s.status() != StageStatus.DONE)
                .mapToInt(GeneralStageResult::weeks)
                .sum();

        List<TaskExposure> tasks = profession.tasks().stream()
                .sorted(Comparator.comparingDouble(ProfessionTask::automation).reversed())
                .map(t -> new TaskExposure(t.name(), t.share(), (int) Math.round(t.automation() * 100)))
                .toList();

        return new AssessmentResult(
                profession.id(),
                profession.name(),
                replaceability,
                exposure,
                adjustment,
                band,
                headline(band),
                readinessScore,
                readinessLevel(readinessScore),
                tasks,
                profession.humanEdge(),
                profession.aiTools(),
                profession.pivotRoles(),
                general,
                profession.roadmap(),
                remainingWeeks,
                null
        );
    }

    private List<GeneralStageResult> generalRoadmap(int readiness, int weeklyHours) {
        List<GeneralStageResult> result = new ArrayList<>();
        boolean currentAssigned = false;
        for (GeneralStage stage : catalog.generalRoadmap()) {
            StageStatus status;
            if (readiness >= stage.doneAtReadiness()) {
                status = StageStatus.DONE;
            } else if (!currentAssigned) {
                status = StageStatus.CURRENT;
                currentAssigned = true;
            } else {
                status = StageStatus.UPCOMING;
            }
            int weeks = Math.max(1, (int) Math.ceil((double) stage.effortHours() / weeklyHours));
            result.add(new GeneralStageResult(stage.id(), stage.title(), stage.goal(), status, weeks,
                    stage.items(), stage.resources()));
        }
        return result;
    }

    static RiskBand band(int replaceability) {
        if (replaceability < 30) {
            return RiskBand.LOW;
        }
        if (replaceability < 50) {
            return RiskBand.MODERATE;
        }
        if (replaceability < 70) {
            return RiskBand.HIGH;
        }
        return RiskBand.VERY_HIGH;
    }

    private static String headline(RiskBand band) {
        return switch (band) {
            case LOW -> "The core of your work stays human. For you, AI is mainly an amplifier.";
            case MODERATE -> "Some of your tasks will be automated. People who use AI will have the edge.";
            case HIGH -> "Many of your tasks are within reach of AI. The role will change, so it pays to act now.";
            case VERY_HIGH -> "Your current mix of tasks is highly exposed to AI. Start planning how your role will shift now.";
        };
    }

    private static String readinessLevel(int readiness) {
        if (readiness < 25) {
            return "Beginner";
        }
        if (readiness < 45) {
            return "Explorer";
        }
        if (readiness < 65) {
            return "Daily user";
        }
        if (readiness < 85) {
            return "Advanced";
        }
        return "Builder";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
