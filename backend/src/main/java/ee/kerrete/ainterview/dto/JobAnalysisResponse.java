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
     * Sobivuse skoor (0–1 või 0–100, sõltuvalt mudeli vastusest).
     * Kui OpenAI tagastab välja "score", seotakse see JsonAlias abil.
     */
    @JsonAlias({"score"})
    private Double matchScore;

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
}
