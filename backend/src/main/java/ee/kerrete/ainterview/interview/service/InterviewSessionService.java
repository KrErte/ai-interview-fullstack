package ee.kerrete.ainterview.interview.service;

import ee.kerrete.ainterview.interview.dto.InterviewAnswerRequestDto;
import ee.kerrete.ainterview.interview.dto.InterviewNextQuestionResponseDto;
import ee.kerrete.ainterview.interview.dto.InterviewQuestionResponseDto;
import ee.kerrete.ainterview.interview.dto.InterviewStartRequestDto;
import ee.kerrete.ainterview.interview.dto.InterviewSummaryResponseDto;
import ee.kerrete.ainterview.interview.enums.InterviewerStyle;
import ee.kerrete.ainterview.interview.model.InterviewAnswer;
import ee.kerrete.ainterview.interview.model.InterviewQuestion;
import ee.kerrete.ainterview.interview.model.InterviewSessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service("statefulInterviewSessionService")
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionService {

    private static final int TOTAL_QUESTIONS = 8;
    private final Map<UUID, InterviewSessionState> sessions = new ConcurrentHashMap<>();

    private final InterviewQuestionGenerator questionGenerator;
    private final AnswerAnalysisService analysisService;
    private final InterviewSummaryBuilder summaryBuilder;

    public InterviewQuestionResponseDto startSession(InterviewStartRequestDto request) {
        validateStart(request);
        UUID sessionId = UUID.randomUUID();
        List<InterviewQuestion> questions = questionGenerator.generate(
            request.getCompanyName(),
            request.getRoleTitle(),
            request.getSeniority(),
            request.getInterviewerStyle(),
            TOTAL_QUESTIONS
        );

        InterviewSessionState state = InterviewSessionState.builder()
            .sessionId(sessionId)
            .candidateEmail(request.getCandidateEmail())
            .companyName(request.getCompanyName())
            .roleTitle(request.getRoleTitle())
            .seniority(request.getSeniority())
            .interviewerStyle(request.getInterviewerStyle())
            .totalQuestions(TOTAL_QUESTIONS)
            .questions(questions)
            .createdAt(Instant.now())
            .lastUpdatedAt(Instant.now())
            .build();

        sessions.put(sessionId, state);

        InterviewQuestion first = questions.get(0);
        return InterviewQuestionResponseDto.builder()
            .sessionId(sessionId)
            .questionNumber(1)
            .totalQuestions(TOTAL_QUESTIONS)
            .question(first.getQuestion())
            .modelAnswerHint(first.getModelAnswerHint())
            .build();
    }

    public Object submitAnswer(InterviewAnswerRequestDto request) {
        InterviewSessionState state = sessions.get(request.getSessionId());
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        int expectedQuestionIndex = state.getAnswers().size();
        if (request.getQuestionNumber() == null || request.getQuestionNumber() != expectedQuestionIndex + 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question number mismatch");
        }
        if (!StringUtils.hasText(request.getAnswer())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer must not be empty");
        }

        InterviewQuestion currentQuestion = state.getQuestions().get(expectedQuestionIndex);
        InterviewAnswer answer = InterviewAnswer.builder()
            .questionNumber(request.getQuestionNumber())
            .question(currentQuestion.getQuestion())
            .answer(request.getAnswer().trim())
            .answeredAt(Instant.now())
            .build();

        InterviewSessionState updated = state.appendAnswer(answer);
        sessions.put(state.getSessionId(), updated);

        boolean finished = updated.getAnswers().size() >= updated.getTotalQuestions();
        if (finished) {
            return summaryBuilder.buildSummaryResponse(updated);
        }

        InterviewQuestion next = updated.getQuestions().get(updated.getAnswers().size());
        InterviewNextQuestionResponseDto.LocalAnalysisDto local = analysisService.localAnalysis(updated.getAnswers());

        return InterviewNextQuestionResponseDto.builder()
            .sessionId(updated.getSessionId())
            .finished(false)
            .questionNumber(updated.getAnswers().size() + 1)
            .totalQuestions(updated.getTotalQuestions())
            .question(next.getQuestion())
            .modelAnswerHint(next.getModelAnswerHint())
            .localAnalysis(local)
            .build();
    }

    private void validateStart(InterviewStartRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is required");
        }
        if (!StringUtils.hasText(request.getCompanyName()) ||
            !StringUtils.hasText(request.getRoleTitle()) ||
            !StringUtils.hasText(request.getSeniority())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company, role, and seniority are required");
        }
        if (request.getInterviewerStyle() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interviewer style is required");
        }
        // allow only defined styles to be safe even if enum changes
        if (!List.of(InterviewerStyle.values()).contains(request.getInterviewerStyle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported interviewer style");
        }
    }
}

