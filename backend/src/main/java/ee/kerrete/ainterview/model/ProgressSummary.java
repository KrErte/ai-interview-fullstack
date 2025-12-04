package ee.kerrete.ainterview.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Treeningprogressi kokkuvõte, mida kasutavad /user/progress ja profiili vaated.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressSummary {

    /**
     * Hinnatud vastuste koguarv.
     */
    private int totalQuestions;

    /**
     * Keskmine skoor (0–100).
     */
    private double averageScore;

    /**
     * Viimase vastuse skoor (0–100, täisarvuna).
     */
    private Integer lastScore;

    /**
     * Kõrgeim saavutatud skoor (0–100, täisarvuna).
     */
    private Integer bestScore;

    /**
     * Üldine progress protsentides (0–100).
     */
    private double progressPercent;

    /**
     * Lipud UI jaoks.
     */
    private boolean canStartMockInterview;
    private boolean canRequestPdf;
}
