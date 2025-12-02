package ee.krerte.aiinterview.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class TrainingProgressResponse {

    int totalJobAnalyses;

    int totalTrainingSessions;

    Integer trainingProgressPercent; // 0–100 (me tagastame alati mitte-null)

    LocalDateTime lastActive;

    Integer lastMatchScore;
    String lastMatchSummary;

    List<String> lastTrainerStrengths;
    List<String> lastTrainerWeaknesses;
}
