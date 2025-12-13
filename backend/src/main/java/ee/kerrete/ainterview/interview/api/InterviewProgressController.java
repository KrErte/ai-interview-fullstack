package ee.kerrete.ainterview.interview.api;

import ee.kerrete.ainterview.interview.dto.InterviewNextQuestionRequestDto;
import ee.kerrete.ainterview.interview.dto.InterviewIntelligenceResponseDto;
import ee.kerrete.ainterview.interview.service.InterviewIntelligenceService;
import ee.kerrete.ainterview.dto.CreateInterviewSessionRequest;
import ee.kerrete.ainterview.dto.CreateInterviewSessionResponse;
import ee.kerrete.ainterview.service.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewProgressController {

    private final InterviewIntelligenceService interviewProgressService;
    private final InterviewSessionService interviewSessionService;
    private final ObjectMapper objectMapper;

    @PostMapping(
        value = "/{sessionId}/next-question",
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public InterviewIntelligenceResponseDto nextQuestion(@PathVariable("sessionId") UUID sessionUuid,
                                                         @RequestBody(required = false) String body) {
        InterviewNextQuestionRequestDto dto = parseAnswer(body);
        return interviewProgressService.nextQuestion(sessionUuid, dto);
    }

    @PostMapping("/start")
    public CreateInterviewSessionResponse start(@RequestBody CreateInterviewSessionRequest request) {
        return interviewSessionService.createSession(request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadJson() {
        return Map.of("error", "Invalid JSON. Expected: {\"answer\":\"...\"}");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleUuidFormat(MethodArgumentTypeMismatchException ex) {
        if (UUID.class.equals(ex.getRequiredType())) {
            return Map.of("error", "Invalid session UUID format");
        }
        return Map.of("error", "Invalid request");
    }

    private InterviewNextQuestionRequestDto parseAnswer(String body) {
        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON. Expected: {\"answer\":\"...\"}");
        }
        try {
            return objectMapper.readValue(body, InterviewNextQuestionRequestDto.class);
        } catch (JsonProcessingException e) {
            // fallback: treat raw body as plain text answer
            return new InterviewNextQuestionRequestDto(body.trim());
        }
    }
}

