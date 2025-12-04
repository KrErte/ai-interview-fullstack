package ee.kerrete.ainterview.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserProgressResponse {

    private String email;

    // vastab: totalJobAnalyses: number;
    private long totalJobAnalyses;

    // vastab: totalTrainingSessions: number;
    private int totalTrainingSessions;

    // vastab: lastActive: string | null;
    private LocalDateTime lastActive;

    // vastab: lastMatchScore: number | null;
    private Double lastMatchScore;

    // vastab: lastMatchSummary: string | null;
    private String lastMatchSummary;

    // vastab: lastTrainerStrengths: string[];
    private List<String> lastTrainerStrengths;

    // vastab: lastTrainerWeaknesses: string[];
    private List<String> lastTrainerWeaknesses;

    // vastab: trainingProgressPercent: number | null;
    // JÄÄB Double – frontis 'number'
    private Double trainingProgressPercent;
}
