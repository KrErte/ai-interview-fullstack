package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.SkillMatrixResponse;
import ee.kerrete.ainterview.dto.SkillMatrixResponse.SkillItem;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Arvutab kasutaja skill-matrixi treeneri vastustest (training_task tabelist).
 */
@Service
@RequiredArgsConstructor
public class SkillMatrixService {

    private final TrainingTaskRepository trainingTaskRepository;

    /**
     * Kaardistus: treeneri ülesande taskKey -> skill name + kategooria.
     *
     * Siia saad hiljem lihtsalt juurde panna uusi seoseid.
     */
    private static final Map<String, SkillDefinition> TASK_SKILL_MAP = Map.ofEntries(
            Map.entry("demo-task-1", new SkillDefinition("Self-reflection", "soft")),
            Map.entry("trainer_q1", new SkillDefinition("Conflict Management", "soft")),
            Map.entry("rest_api_design", new SkillDefinition("REST API Design", "backend")),
            Map.entry("system_design_scaling", new SkillDefinition("System Design", "architecture")),
            Map.entry("cloud_aws_basics", new SkillDefinition("Cloud (AWS)", "cloud")),
            Map.entry("ci_cd_pipeline", new SkillDefinition("CI/CD", "devops")),
            Map.entry("observability_intro", new SkillDefinition("Observability", "devops")),
            Map.entry("testing_junit", new SkillDefinition("Automated Testing", "quality")),
            Map.entry("security_basics", new SkillDefinition("App Security", "security")),
            Map.entry("database_tuning", new SkillDefinition("Database Performance", "data")),
            Map.entry("frontend_angular", new SkillDefinition("Angular UI", "frontend")),
            Map.entry("communication_storytelling", new SkillDefinition("Communication", "soft")),
            Map.entry("leadership_growth", new SkillDefinition("Leadership", "soft")),
            Map.entry("ownership_mindset", new SkillDefinition("Ownership", "culture")),
            Map.entry("mentoring_junior", new SkillDefinition("Mentoring", "soft"))
    );

    public SkillMatrixResponse getSkillMatrix(String email) {
        List<TrainingTask> tasks = trainingTaskRepository.findByEmail(email);

        if (tasks.isEmpty()) {
            return SkillMatrixResponse.empty();
        }

        // koondame kasutaja tehtud taskid skilli kaupa
        Map<String, SkillAgg> skillAggMap = new HashMap<>();

        for (TrainingTask task : tasks) {
            SkillDefinition def = TASK_SKILL_MAP.get(task.getTaskKey());
            if (def == null) {
                // selle taskKey jaoks pole skill-mappingut
                continue;
            }

            SkillAgg agg = skillAggMap.computeIfAbsent(
                    def.skillName(),
                    k -> new SkillAgg(def.skillName(), def.category())
            );

            agg.totalTasks++; // mitu korda seda skilli treeniti
            if (task.isCompleted()) {
                agg.completedTasks++;
            }
        }

        if (skillAggMap.isEmpty()) {
            return SkillMatrixResponse.empty();
        }

        // mitu võimalikku ülesannet on iga skilli kohta defineeritud
        Map<String, Long> totalAvailablePerSkill = TASK_SKILL_MAP.values().stream()
                .collect(Collectors.groupingBy(
                        SkillDefinition::skillName,
                        Collectors.counting()
                ));

        List<SkillItem> items = new ArrayList<>();

        for (SkillAgg agg : skillAggMap.values()) {
            long available = totalAvailablePerSkill.getOrDefault(agg.skillName, 1L);
            int level = (int) Math.round(100.0 * agg.completedTasks / (double) available);

            items.add(
                    SkillItem.builder()
                            .skillName(agg.skillName)
                            .category(agg.category)
                            .level(level)
                            .completedTasks(agg.completedTasks)
                            .totalTasks((int) available)
                            .build()
            );
        }

        int avg = (int) Math.round(
                items.stream().mapToInt(SkillItem::getLevel).average().orElse(0.0)
        );

        LevelPair levelPair = mapAverageToLevel(avg);

        // TOP 3 skill-gapi (madalaimad levelid, mis pole 100%)
        List<SkillItem> topGaps = items.stream()
                .filter(i -> i.getLevel() < 100)
                .sorted(Comparator.comparingInt(SkillItem::getLevel))
                .limit(3)
                .collect(Collectors.toList());

        return SkillMatrixResponse.builder()
                .skills(items)
                .averageLevel(avg)
                .currentLevel(levelPair.current())
                .nextLevel(levelPair.next())
                .topGaps(topGaps)
                .build();
    }

    private LevelPair mapAverageToLevel(int avg) {
        if (avg < 40) {
            return new LevelPair("Junior", "Mid-level");
        } else if (avg < 70) {
            return new LevelPair("Mid-level", "Senior");
        } else if (avg < 90) {
            return new LevelPair("Senior", "Architect");
        } else {
            return new LevelPair("Architect", "🏆 Legend");
        }
    }

    // --- abiklassid ---

    private record SkillDefinition(String skillName, String category) {}

    private static class SkillAgg {
        final String skillName;
        final String category;
        int completedTasks = 0;
        int totalTasks = 0;

        SkillAgg(String skillName, String category) {
            this.skillName = skillName;
            this.category = category;
        }
    }

    private record LevelPair(String current, String next) {}
}
