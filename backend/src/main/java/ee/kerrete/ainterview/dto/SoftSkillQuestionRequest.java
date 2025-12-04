package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Päring soft-skill treenerile järgmise küsimuse jaoks.
 * Sama endpointi kasutatakse nii esimese kui ka järgmiste küsimuste jaoks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftSkillQuestionRequest {

    /**
     * Kasutaja email (progressi ja statistika jaoks).
     */
    private String email;

    /**
     * Fookus-roadmapi võti, nt:
     *  - "technical_initiative"
     *  - "clear_communication"
     *  - "conflict_management"
     *  - "ownership"
     */
    private String roadmapKey;

    /**
     * Viimane küsimus, millele kasutaja vastas.
     * Esimese küsimuse puhul võib olla null.
     */
    private String previousQuestion;

    /**
     * Kasutaja viimane vastus. Esimese küsimuse puhul null/tyhi.
     */
    private String previousAnswer;

    /**
     * Senine küsimuste-vastuste ajalugu samas sessioonis
     * (et AI saaks järjepidevust hoida).
     */
    private List<HistoryTurn> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryTurn {
        private String question;
        private String answer;
    }
}
