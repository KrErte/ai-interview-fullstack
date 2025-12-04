package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.CreateInterviewSessionRequest;
import ee.kerrete.ainterview.dto.CreateInterviewSessionResponse;
import ee.kerrete.ainterview.dto.GenerateQuestionsRequest;
import ee.kerrete.ainterview.model.Question;

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

