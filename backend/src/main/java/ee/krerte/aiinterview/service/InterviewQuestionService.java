package ee.krerte.aiinterview.service;

import ee.krerte.aiinterview.dto.CreateInterviewSessionRequest;
import ee.krerte.aiinterview.dto.CreateInterviewSessionResponse;
import ee.krerte.aiinterview.dto.GenerateQuestionsRequest;
import ee.krerte.aiinterview.model.Question;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class InterviewQuestionService {

    private final OpenAiClient openAiClient;

    public List<Question> generateFromCv(GenerateQuestionsRequest request) {
        int tech = request.getTechnicalCount() > 0 ? request.getTechnicalCount() : 8;
        int soft = request.getSoftCount() > 0 ? request.getSoftCount() : 4;

        return openAiClient.generateQuestionsFromCv(
                request.getCvText(), tech, soft
        );
    }
}

