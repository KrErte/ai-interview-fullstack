package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * Kasutaja skill-matrixi vastus.
 *
 * Sisaldab:
 *  - iga skill eraldi (nimi, protsent, kategooria, completed/total)
 *  - keskmine tase %
 *  - sõnaline tase (Junior / Mid-level / Senior / Architect)
 *  - TOP 3 skill-gapi (mida edasi arendada)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillMatrixResponse {

    /**
     * Kõik skilli kirjed.
     */
    private List<SkillItem> skills;

    /**
     * Kõigi skillide keskmine tase (0–100).
     */
    private int averageLevel;

    /**
     * Praegune üldtase (nt "Mid-level").
     */
    private String currentLevel;

    /**
     * Järgmine sihttase (nt "Senior").
     */
    private String nextLevel;

    /**
     * TOP 3 oskust, mida edasi arendada.
     */
    private List<SkillItem> topGaps;

    public static SkillMatrixResponse empty() {
        return SkillMatrixResponse.builder()
                .skills(Collections.emptyList())
                .averageLevel(0)
                .currentLevel("Junior")
                .nextLevel("Mid-level")
                .topGaps(Collections.emptyList())
                .build();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillItem {

        /**
         * Oskuse nimi, nt "Conflict Management", "Java Spring Boot".
         */
        private String skillName;

        /**
         * Tase protsentides (0–100).
         */
        private int level;

        /**
         * Kategooria, nt "soft", "tech", "leadership", "ownership".
         */
        private String category;

        /**
         * Mitu ülesannet sellest skillist on kasutaja ära teinud.
         */
        private int completedTasks;

        /**
         * Mitu võimalikku ülesannet selle skilli jaoks üldse defineeritud on.
         */
        private int totalTasks;
    }
}
