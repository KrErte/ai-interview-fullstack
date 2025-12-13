package ee.kerrete.ainterview.interview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterviewIntelligenceResponseDto {
    String question;
    String decision;
    Integer fitScore;
    String fitTrend;
    Progress progress;
    boolean sessionComplete;
    Fit fit;
    FitBreakdown fitBreakdown;

    @Value
    @Builder
    public static class Progress {
        int questionCount;
        String currentDimension;
        double last1Average;
        double last3Average;
        double last5Average;
    }

    @Value
    @Builder
    public static class Fit {
        Boolean computed;
        Double overall;
        Double currentDimension;
        Double trend;
    }

    @Value
    @Builder
    public static class FitBreakdown {
        String confidence; // LOW|MEDIUM|HIGH
        int answeredCount;
        java.util.List<DimensionBreakdown> dimensions;
    }

    @Value
    @Builder
    public static class DimensionBreakdown {
        String key;
        String label;
        Integer scorePercent;
        String band; // STRONG|GOOD|NEEDS_WORK
        java.util.List<Insight> insights;
    }

    @Value
    @Builder
    public static class Insight {
        String type; // STRENGTH|RISK
        String text;
    }
}

