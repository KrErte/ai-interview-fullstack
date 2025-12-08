package ee.kerrete.ainterview.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatchDto {
    private String jobTitle;
    private String jobDescription;
    private Double fitScore; // 0-100
    private List<String> strengths;
    private List<String> gaps;
    private List<String> roadmap;
    private String summary;
    private String source;
}













