package ee.kerrete.ainterview.interview.api;

import ee.kerrete.ainterview.interview.dto.InterviewNextQuestionRequestDto;
import ee.kerrete.ainterview.interview.dto.InterviewProgressResponseDto;
import ee.kerrete.ainterview.interview.service.InterviewProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    private final InterviewProgressService interviewProgressService;
    private final ObjectMapper objectMapper;

    @PostMapping(
        value = "/{sessionUuid}/next-question",
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public InterviewProgressResponseDto nextQuestion(@PathVariable("sessionUuid") UUID sessionUuid,
                                                     @RequestBody(required = false) String body) {
        String answer = parseAnswer(body);
        return interviewProgressService.nextQuestion(sessionUuid, answer);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadJson() {
        return Map.of("error", "Invalid JSON. Expected: {\"answer\":\"...\"}");
    }

    private String parseAnswer(String body) {
        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON. Expected: {\"answer\":\"...\"}");
        }
        try {
            InterviewNextQuestionRequestDto dto = objectMapper.readValue(body, InterviewNextQuestionRequestDto.class);
            return dto.answer();
        } catch (JsonProcessingException e) {
            // fallback: treat raw body as plain text answer
            return body.trim();
        }
    }
}

