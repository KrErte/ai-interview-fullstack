package ee.kerrete.ainterview.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.interview.dto.InterviewProgressResponseDto;
import ee.kerrete.ainterview.interview.enums.InterviewerStyle;
import ee.kerrete.ainterview.model.InterviewSession;
import ee.kerrete.ainterview.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewProgressService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final ObjectMapper objectMapper;

    public InterviewProgressResponseDto nextQuestion(UUID sessionUuid, String answer) {
        if (sessionUuid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session id is required");
        }
        if (!StringUtils.hasText(answer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer must not be blank");
        }

        InterviewSession session = interviewSessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        String trimmedAnswer = answer.trim();
        List<String> last3 = parseList(session.getLast3Answers());
        List<String> last5 = parseList(session.getLast5Answers());

        List<String> updatedLast3 = appendCapped(last3, trimmedAnswer, 3);
        List<String> updatedLast5 = appendCapped(last5, trimmedAnswer, 5);

        InterviewerStyle style = Optional.ofNullable(session.getInterviewerStyle()).orElse(InterviewerStyle.HR);

        session.setInterviewerStyle(style);
        session.setLast1Answer(trimmedAnswer);
        session.setLast3Answers(writeList(updatedLast3));
        session.setLast5Answers(writeList(updatedLast5));
        interviewSessionRepository.save(session);

        String nextQuestion = generateNextQuestion(style, trimmedAnswer, updatedLast3, updatedLast5);

        InterviewProgressResponseDto.UsedContext usedContext = InterviewProgressResponseDto.UsedContext.builder()
            .lastAnswer(trimmedAnswer)
            .last3(updatedLast3)
            .last5(updatedLast5)
            .build();

        return InterviewProgressResponseDto.builder()
            .question(nextQuestion)
            .interviewerStyle(style)
            .usedContext(usedContext)
            .build();
    }

    private List<String> appendCapped(List<String> original, String newest, int cap) {
        List<String> result = new ArrayList<>(original == null ? List.of() : original);
        result.add(newest);
        if (result.size() > cap) {
            int start = result.size() - cap;
            return new ArrayList<>(result.subList(start, result.size()));
        }
        return result;
    }

    private List<String> parseList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String generateNextQuestion(InterviewerStyle style, String lastAnswer, List<String> last3, List<String> last5) {
        String focus = switch (style) {
            case TECH -> "technical depth";
            case TEAM_LEAD -> "leadership and collaboration";
            case MIXED -> "a balanced view of impact and teamwork";
            case HR -> "values alignment and motivation";
        };

        String recent = StringUtils.hasText(lastAnswer) ? lastAnswer : "your recent experience";
        String recent3 = last3.isEmpty() ? "" : " Recent themes: " + String.join(" | ", last3);
        String recent5 = last5.isEmpty() ? "" : " Broader context: " + String.join(" | ", last5);

        return "From a " + focus + " angle, could you expand on \"" + recent + "\"?" + recent3 + recent5;
    }
}

