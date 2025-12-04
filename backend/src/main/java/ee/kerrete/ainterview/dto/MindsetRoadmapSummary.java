package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vasakul sidebaris kuvatava mindset-roadmapi kokkuvõte.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindsetRoadmapSummary {

    /**
     * Stabiilne võti (nt "conflict", "leadership").
     * Front kasutab seda URL-is ja valiku salvestamiseks.
     */
    private String roadmapKey;

    /**
     * Inimloetav nimi (nt "Conflict Management").
     */
    private String title;

    private int totalTasks;
    private int completedTasks;
    private int progressPercent;
}
