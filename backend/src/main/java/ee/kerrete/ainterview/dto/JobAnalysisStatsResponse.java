package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JobAnalysisStatsResponse {

    private long totalAnalyses;
    private long totalAnalysesForEmail;
    private Double lastMatchScoreForEmail;
    private String lastSummaryForEmail;
}
