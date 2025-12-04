package ee.kerrete.ainterview.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vastuse hindamise DTO, mida frontend kasutab.
 *
 * Kasutusnäide vanast koodist:
 * new EvaluateAnswerResponse(
 *     eval.getScore(),
 *     eval.getStrengths(),
 *     eval.getWeaknesses(),
 *     eval.getSuggestions()
 * )
 *
 * NB! Siin on spetsiaalselt jäetud 4-arg konstruktor,
 * et vana kood ei katki läheks. Uus väli "fallback"
 * on vaikimisi false.
 */
@Getter
@Setter
@NoArgsConstructor
public class EvaluateAnswerResponse {

    /**
     * Skoor 0–100 (AI hinnang vastusele).
     */
    private int score;

    /**
     * Mis läks vastuses hästi / tugevused.
     */
    private String strengths;

    /**
     * Nõrkused või mis jäi puudu.
     */
    private String weaknesses;

    /**
     * Soovitused, mida järgmiseks parandada / harjutada.
     */
    private String suggestions;

    /**
     * Kas kasutati fallback-hindamist (true), mitte OpenAI päris hindamist.
     */
    private boolean fallback;

    /**
     * Vana 4-arg konstruktor, et olemasolevad kutsed ei katkeks.
     * Fallback jääb vaikimisi false.
     */
    public EvaluateAnswerResponse(int score,
                                  String strengths,
                                  String weaknesses,
                                  String suggestions) {
        this.score = score;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.suggestions = suggestions;
        this.fallback = false;
    }
}
