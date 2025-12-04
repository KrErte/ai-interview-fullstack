package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vastus soft-skill treenerilt.
 *  - järgmine küsimus
 *  - AI hinnang eelmisele vastusele (score + coachFeedback)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftSkillQuestionResponse {

    /**
     * Järgmine treeneri küsimus, mida front kuvab.
     */
    private String questionText;

    /**
     * "easy", "medium" või "hard" – informatiivne raskusaste.
     */
    private String difficulty;

    /**
     * true = kasutati fallback'i (OpenAI vastus oli vigane või puudus),
     * false = päris OpenAI-ga genereeritud küsimus.
     */
    private boolean fallback;

    /**
     * AI poolt arvutatud keskmine skoor eelmisele vastusele (0–100).
     * Kui oli esimene küsimus või analüüs kukkus läbi, võib olla null.
     */
    private Integer score;

    /**
     * Coaching-tagasiside tekst eelmise vastuse kohta (kui olemas).
     */
    private String coachFeedback;

    /**
     * AI hinnangul nõrgim skill (nt "conflict_management"), kui selline leiti.
     */
    private String weakestSkill;
}
