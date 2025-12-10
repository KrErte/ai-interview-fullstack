package ee.kerrete.ainterview.interview.api;

import ee.kerrete.ainterview.interview.dto.InterviewSimulationRequest;
import ee.kerrete.ainterview.interview.dto.InterviewSimulationResponse;
import ee.kerrete.ainterview.interview.service.InterviewSimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewSimulationService interviewSimulationService;

    @PostMapping("/simulate")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewSimulationResponse simulate(@Valid @RequestBody InterviewSimulationRequest request) {
        return interviewSimulationService.simulate(request);
    }
}

