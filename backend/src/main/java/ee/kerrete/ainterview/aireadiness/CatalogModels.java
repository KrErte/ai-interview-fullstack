package ee.kerrete.ainterview.aireadiness;

import java.util.List;

/**
 * Staatiline sisu (ametid, küsimused, üldine roadmap), mis loetakse classpath'ilt JSON-ist.
 */
public final class CatalogModels {

    private CatalogModels() {
    }

    public record ProfessionTask(String name, int share, double automation) {
    }

    public record RoadmapPhase(String title, List<String> items, String project) {
    }

    public record Profession(
            String id,
            String name,
            String category,
            List<ProfessionTask> tasks,
            List<String> humanEdge,
            List<String> aiTools,
            List<String> pivotRoles,
            List<RoadmapPhase> roadmap
    ) {
    }

    public record QuestionOption(String id, String label, int risk, int readiness, Integer weeklyHours) {
    }

    public record Question(String id, String text, String help, List<QuestionOption> options) {
    }

    public record Resource(String title, String url) {
    }

    public record GeneralStage(
            String id,
            String title,
            String goal,
            int effortHours,
            int doneAtReadiness,
            List<String> items,
            List<Resource> resources
    ) {
    }
}
