package ee.kerrete.ainterview.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
     * Match score: ALWAYS an integer 0–100 in the /api/job-analysis JSON (FP-20 contract, e.g. 83, never 83.0).
     * The service reads the model's raw number itself and normalizes it ({@code JobAnalysisService.normalizeScore}):
     * only values below 1 are fractions (x100), then clamp 0–100 and round.
     * Kui OpenAI tagastab välja "score", seotakse see JsonAlias abil.
     */
    @JsonAlias({"score"})
    private Integer matchScore;

    /**
     * Kandidaadi tugevused võrreldes töökuulutusega.
     */
    private List<String> strengths;

    /**
     * Nõrkused või riskikohad võrreldes töökuulutusega.
     */
    private List<String> weaknesses;

    /**
     * Oskused, mis töökuulutuse järgi puudu jäävad.
     */
    private List<String> missingSkills;

    /**
     * Soovituslik roadmap (mida õppida / arendada).
     */
    private List<String> roadmap;

    /**
     * Kokkuvõtlikud soovitused CV/oskuste parandamiseks (tekstina).
     */
    private String suggestedImprovements;

    /**
     * Lühike kokkuvõte (säilitame, et profiili ülevaated jääksid toimima).
     */
    private String summary;

    /**
     * true when this is the local heuristic analysis (no OpenAI key or the AI call failed) instead of
     * an AI analysis (FP-20).
     */
    private boolean fallback;
}
