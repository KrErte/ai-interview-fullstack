package ee.kerrete.ainterview.interview.api;

import ee.kerrete.ainterview.interview.dto.InterviewAnswerRequestDto;
import ee.kerrete.ainterview.interview.dto.InterviewNextQuestionResponseDto;
import ee.kerrete.ainterview.interview.dto.InterviewQuestionResponseDto;
import ee.kerrete.ainterview.interview.dto.InterviewStartRequestDto;
import ee.kerrete.ainterview.interview.service.InterviewSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview/session")
@RequiredArgsConstructor
public class InterviewSessionFlowController {

    private final InterviewSessionService sessionService;

    @PostMapping("/start")
    public InterviewQuestionResponseDto start(@Valid @RequestBody InterviewStartRequestDto request) {
        return sessionService.startSession(request);
    }

    @PostMapping("/answer")
    public Object answer(@Valid @RequestBody InterviewAnswerRequestDto request) {
        return sessionService.submitAnswer(request);
    }
}

