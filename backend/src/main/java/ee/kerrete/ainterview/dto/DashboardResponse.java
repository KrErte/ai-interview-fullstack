package ee.kerrete.ainterview.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private String email;
    private String fullName;
    private String targetRole;
    private Integer profileCompleteness;
    private Double latestFitScore;
    private Integer totalTrainingTasks;
    private Integer completedTrainingTasks;
    private Integer trainingProgressPercent;
    private boolean cvUploaded;
    private String lastAnalysisSummary;
    private String lastActive;
}

