package ee.krerte.aiinterview.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Vastus Job Matcheri analüüsile.
 */
@Getter
@Setter
public class JobAnalysisResponse {

    /**
     * Sobivuse skoor (0–100).
     * Seda kasutavad:
     *  - JobAnalysisService (getScore/setScore)
     *  - JobAnalysisSessionService
     *  - JobAnalysisStatsService (profiili statistika jaoks)
     */
    private Double score;

    /**
     * Lühike kokkuvõte, kuidas CV ja töökuulutus sobivad.
     */
    private String summary;

    /**
     * Osad oskused, mis töökuulutuse järgi puudu jäävad.
     */
    private List<String> missingSkills;

    /**
     * Soovituslik roadmap (mida õppida / arendada).
     */
    private List<String> roadmap;

    /**
     * Konkreetsed soovitused CV ja profiili paremaks muutmiseks.
     */
    private List<String> suggestedImprovements;
}
