package ee.kerrete.ainterview.recruiter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterCandidateSummaryDto {

    private String id;
    private String name;
    private String email;
    private double matchScore;
    private String latestScoreLabel;
    private List<String> keySkills;
    private int analysesRun;
    private int trainingSessions;
    private int trainingProgressPercent;
    private Instant lastUpdated;
}

