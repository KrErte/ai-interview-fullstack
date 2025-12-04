package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vastus adaptiivse küsimuse genereerimisele.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveQuestionResponse {

    /**
     * Järgmine treeneri küsimus, mille OpenAI genereeris.
     */
    private String questionText;

    /**
     * true = kasutati fallback'i (OpenAI jamas),
     * false = päris OpenAI küsimus.
     */
    private boolean fallback;
}
