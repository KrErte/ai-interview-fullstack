package ee.kerrete.ainterview.aireadiness;

import java.util.List;

/**
 * Staatiline sisu (ametid, küsimused, üldine roadmap), mis loetakse classpath'ilt JSON-ist.
 */
public final class CatalogModels {

    private CatalogModels() {
    }

    /** observedUse: AI-d kasutatakse selle ülesande jaoks juba päriselt (Anthropic Economic Index). */
    public record ProfessionTask(String name, int share, double automation, boolean observedUse) {

        public ProfessionTask(String name, int share, double automation) {
            this(name, share, automation, false);
        }
    }

    public record RoadmapPhase(String title, List<String> items, String project) {
    }

    /**
     * Käsitsi kirjutatud ametil on onetCodes (O*NET ametid, mida see katab ja mis peidetakse).
     * O*NET-ist genereeritud ametil on onetCode ja alternateTitles (ainult otsingu jaoks).
     */
    public record Profession(
            String id,
            String name,
            String category,
            List<ProfessionTask> tasks,
            List<String> humanEdge,
            List<String> aiTools,
            List<String> pivotRoles,
            List<RoadmapPhase> roadmap,
            String onetCode,
            List<String> onetCodes,
            List<String> alternateTitles
    ) {

        public Profession(String id, String name, String category, List<ProfessionTask> tasks,
                          List<String> humanEdge, List<String> aiTools, List<String> pivotRoles,
                          List<RoadmapPhase> roadmap) {
            this(id, name, category, tasks, humanEdge, aiTools, pivotRoles, roadmap, null, List.of(), List.of());
        }

        public Profession {
            onetCodes = onetCodes == null ? List.of() : List.copyOf(onetCodes);
            alternateTitles = alternateTitles == null ? List.of() : List.copyOf(alternateTitles);
        }
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
