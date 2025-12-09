package ee.kerrete.ainterview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Päring vastuse hindamiseks (nii intervjueerija kui treeneri vaatele).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateAnswerRequest {

    /**
     * Kasutaja email – vaja sessioonide / statistika jaoks.
     */
    private String email;

    /**
     * Küsimuse ID (võib olla null, kui ei kasutata).
     */
    private String questionId;

    /**
     * Küsimuse tekst.
     */
    private String question;

    /**
     * Kasutaja vastus (STAR meetodil).
     */
    @NotBlank
    private String answer;
}
