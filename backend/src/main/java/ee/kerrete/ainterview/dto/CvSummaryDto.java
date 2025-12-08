package ee.kerrete.ainterview.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvSummaryDto {
    private String email;
    private String headline;
    private List<String> parsedSkills;
    private String experienceSummary;
    private String rawText;
}













