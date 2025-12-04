package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.SoftSkillMatrixResponse;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Arvutab soft-skill “matrixi” kasutaja treeningvastuste põhjal.
 *
 * Praegune lihtne heuristika:
 *  - kasutab TrainingTask.score (0–100)
 *  - jaotab ülesanded dimensioonidesse taskKey järgi
 *    (nt "conflict_", "lead_", "reflect_", "safety_" jne)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SoftSkillMatrixService {

    private final TrainingTaskRepository trainingTaskRepository;

    public SoftSkillMatrixResponse calculateFor(String email) {
        List<TrainingTask> tasks = trainingTaskRepository.findByEmail(email);

        if (tasks == null || tasks.isEmpty()) {
            log.info("Soft-skill matrix requested for email='{}', but no TrainingTask records found.", email);
            return SoftSkillMatrixResponse.empty(email);
        }

        // Ainult need, millel on skoor
        List<TrainingTask> withScore = tasks.stream()
                .filter(t -> t.getScore() != null)
                .toList();

        if (withScore.isEmpty()) {
            log.info("Soft-skill matrix: email='{}' has tasks but no scores set.", email);
            return SoftSkillMatrixResponse.empty(email);
        }

        // Dimensioonide keskmised skaalal 0–5
        double star = averageOnScale(withScore, this::isStarTask);
        double conflict = averageOnScale(withScore, this::isConflictTask);
        double emo = averageOnScale(withScore, this::isEmotionalIntelligenceTask);
        double leadership = averageOnScale(withScore, this::isLeadershipTask);
        double reflection = averageOnScale(withScore, this::isReflectionTask);
        double safety = averageOnScale(withScore, this::isPsychSafetyTask);

        // Kui STAR on 0, kasuta fallbackina kõigi vastuste keskmist
        if (star == 0.0) {
            star = averageOnScale(withScore, t -> true);
        }

        double overallScore = computeOverallScore(star, conflict, emo, leadership, reflection, safety);
        String level = mapLevel(overallScore);

        LocalDateTime last = withScore.stream()
                .map(TrainingTask::getUpdatedAt)
                .filter(d -> d != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return SoftSkillMatrixResponse.builder()
                .email(email)
                .starStructure(round1(star))
                .conflictResolution(round1(conflict))
                .emotionalIntelligence(round1(emo))
                .leadership(round1(leadership))
                .reflection(round1(reflection))
                .psychologicalSafety(round1(safety))
                .overallScore(round1(overallScore))
                .overallLevel(level)
                .totalAnswers(withScore.size())
                .lastUpdated(last != null ? last.toString() : null)
                .build();
    }

    private double averageOnScale(List<TrainingTask> tasks, Predicate<TrainingTask> filter) {
        DoubleSummaryStatistics stats = tasks.stream()
                .filter(filter)
                .map(TrainingTask::getScore)
                .mapToDouble(Integer::doubleValue)
                .summaryStatistics();

        if (stats.getCount() == 0) {
            return 0.0;
        }

        double avgScore0to100 = stats.getAverage();
        // 0–100 → 0–5
        return avgScore0to100 / 20.0;
    }

    private double computeOverallScore(double... dims0to5) {
        double sum = 0.0;
        int count = 0;
        for (double d : dims0to5) {
            if (d > 0.0) {
                sum += d;
                count++;
            }
        }
        if (count == 0) {
            return 0.0;
        }
        double avg0to5 = sum / count;
        return avg0to5 * 20.0; // tagasi 0–100 skaalale
    }

    private String mapLevel(double overallScore) {
        if (overallScore <= 0.0) {
            return "NO_DATA";
        } else if (overallScore < 50.0) {
            return "JUNIOR";
        } else if (overallScore < 75.0) {
            return "MID";
        } else if (overallScore < 90.0) {
            return "SENIOR_READY";
        } else {
            return "LEAD_POTENTIAL";
        }
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // ---- Heuristikad taskKey põhjal (võid hiljem kohandada) ----

    private boolean isStarTask(TrainingTask t) {
        String key = normalizeKey(t.getTaskKey());
        return key.contains("star") || key.contains("general") || key.contains("story") || key.startsWith("trainer_q");
    }

    private boolean isConflictTask(TrainingTask t) {
        String key = normalizeKey(t.getTaskKey());
        return key.contains("conflict") || key.contains("dispute") || key.contains("difficult_person");
    }

    private boolean isEmotionalIntelligenceTask(TrainingTask t) {
        String key = normalizeKey(t.getTaskKey());
        return key.contains("emotion") || key.contains("empathy") || key.contains("feedback");
    }

    private boolean isLeadershipTask(TrainingTask t) {
        String key = normalizeKey(t.getTaskKey());
        return key.contains("lead") || key.contains("ownership") || key.contains("initiative");
    }

    private boolean isReflectionTask(TrainingTask t) {
        String key = normalizeKey(t.getTaskKey());
        return key.contains("reflect") || key.contains("learned") || key.contains("mistake");
    }

    private boolean isPsychSafetyTask(TrainingTask t) {
        String key = normalizeKey(t.getTaskKey());
        return key.contains("safety") || key.contains("trust") || key.contains("team_climate");
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT);
    }
}
