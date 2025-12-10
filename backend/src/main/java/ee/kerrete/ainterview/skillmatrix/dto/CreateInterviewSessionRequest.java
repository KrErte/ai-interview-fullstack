package ee.kerrete.ainterview.skillmatrix.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateInterviewSessionRequest {
    private String candidateEmail;
    private String candidateId;
    private String positionId;
    private String jobId;
    private String skillMatrixId;
    private List<String> experimentKeys;
}


