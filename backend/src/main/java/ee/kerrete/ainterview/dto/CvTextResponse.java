package ee.kerrete.ainterview.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvTextResponse {
    private String text;
    private String headline;
    private List<String> skills;
    private String experienceSummary;
}
