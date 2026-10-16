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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return score(profession, answers, null);
    }

    /**
     * Nagu score(profession, answers), aga taskShares (kui antud) asendab kataloogi osakaalud selle
     * inimese nädalaga. Automatiseeritavus ja vastustest tulev nihe ei muutu.
     */
    public AssessmentResult score(Profession profession, Map<String, String> answers, Map<String, Integer> taskShares) {
        Map<String, Integer> shares = shares(profession, taskShares);
        int exposure = (int) Math.round(profession.tasks().stream()
                .mapToDouble(t -> shares.get(t.name()) * t.automation())
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
                .map(t -> new TaskExposure(t.name(), shares.get(t.name()), (int) Math.round(t.automation() * 100), t.observedUse()))
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

    /**
     * Ülesande nimi -> osakaal. Ilma override'ita kataloogi osakaalud. Override peab sisaldama täpselt
     * selle ameti ülesandeid, iga osakaal on 0..100 ja summa 100; muidu IllegalArgumentException (400).
     */
    static Map<String, Integer> shares(Profession profession, Map<String, Integer> taskShares) {
        Map<String, Integer> shares = new LinkedHashMap<>();
        if (taskShares == null) {
            profession.tasks().forEach(t -> shares.put(t.name(), t.share()));
            return shares;
        }
        Set<String> names = new HashSet<>();
        profession.tasks().forEach(t -> names.add(t.name()));
        for (String name : taskShares.keySet()) {
            if (!names.contains(name)) {
                throw new IllegalArgumentException("Unknown task '" + name + "' for profession '" + profession.id() + "'");
            }
        }
        int sum = 0;
        for (ProfessionTask t : profession.tasks()) {
            Integer share = taskShares.get(t.name());
            if (share == null) {
                throw new IllegalArgumentException("Missing share for task '" + t.name() + "'");
            }
            if (share < 0 || share > 100) {
                throw new IllegalArgumentException("Share for task '" + t.name() + "' must be 0..100");
            }
            shares.put(t.name(), share);
            sum += share;
        }
        if (sum != 100) {
            throw new IllegalArgumentException("Task shares must add up to 100, not " + sum);
        }
        return shares;
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
