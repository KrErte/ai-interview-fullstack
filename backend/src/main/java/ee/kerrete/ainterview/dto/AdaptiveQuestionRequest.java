package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adaptive Treeneri küsimuse genereerimise päring.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveQuestionRequest {

    /**
     * Kasutaja eelmine vabatekstiline vastus (STAR).
     */
    private String previousAnswer;

    /**
     * Kasutaja email (valikuline, progressi jaoks).
     */
    private String email;
}
