package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Profiili ülevaate DTO, mida kasutab Minu profiil vaade.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileOverviewResponse {

    /**
     * Kõigi Job Matcheri analüüside koguarv (kõigi kasutajate peale).
     */
    private int totalAnalyses;

    /**
     * Antud emailiga tehtud analüüside arv.
     */
    private int totalAnalysesForEmail;

    /**
     * Viimase analüüsi sobivuse skoor (0–100).
     */
    private Double lastMatchScoreForEmail;

    /**
     * Viimase analüüsi AI kokkuvõte.
     */
    private String lastSummaryForEmail;

    /**
     * Viimane aktiivsus (ISO-8601 stringina).
     */
    private String lastActive;

    /**
     * TREENERI sessioonide arv – kui mitu AI treening-vastust on antud.
     */
    private int trainingSessionCount; // <-- SEE LISA!
}
