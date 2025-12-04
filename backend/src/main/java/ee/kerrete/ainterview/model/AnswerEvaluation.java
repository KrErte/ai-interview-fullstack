package ee.kerrete.ainterview.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Vastuse hinnang, mida kasutavad:
 *  - EvaluationService (OpenAI põhine tööpakkumise analüüs)
 *  - AnswerEvaluationService (treeneri vastuse hindamine)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerEvaluation {

    /**
     * Küsimus, millele vastati (treeneri puhul).
     */
    private String question;

    /**
     * Kandidaadi vastus (toor tekst).
     */
    private String answer;

    /**
     * Hinne 0–100 (täisarv).
     */
    private Integer score;

    /**
     * Lühike kokkuvõte / tagasiside.
     */
    private String feedback;

    /**
     * Tugevused, mida AI vastusest välja tõi.
     */
    private List<String> strengths;

    /**
     * Nõrkused vastuses.
     */
    private List<String> weaknesses;

    /**
     * Soovitused parandamiseks.
     */
    private List<String> suggestions;

    /**
     * Loomise aeg.
     */
    private LocalDateTime createdAt;

    /**
     * Helper, mida kasutab EvaluationService – loeb OpenAI JSON-i mudelisse.
     * Kui JSON ei vasta mudelile, pannakse kogu sisu feedbacki välja
     * ning teised väljad jäävad vaikimisi väärtustele.
     */
    public static AnswerEvaluation fromJson(String json) {
        if (json == null || json.isBlank()) {
            return AnswerEvaluation.builder()
                    .score(0)
                    .feedback("")
                    .strengths(Collections.emptyList())
                    .weaknesses(Collections.emptyList())
                    .suggestions(Collections.emptyList())
                    .build();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, AnswerEvaluation.class);
        } catch (Exception ex) {
            // kui parsimine ebaõnnestub, pane kogu json feedbacki alla
            AnswerEvaluation eval = new AnswerEvaluation();
            eval.setScore(0);
            eval.setFeedback(json);
            eval.setStrengths(Collections.emptyList());
            eval.setWeaknesses(Collections.emptyList());
            eval.setSuggestions(Collections.emptyList());
            return eval;
        }
    }
}
