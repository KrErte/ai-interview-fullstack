package ee.krerte.aiinterview.api;

import ee.krerte.aiinterview.dto.CvQuestionsRequest;
import ee.krerte.aiinterview.model.Question;
import ee.krerte.aiinterview.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")   // <-- /api/questions
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuestionController {

    private final QuestionService questionService;

    /**
     * FRONTEND kutsub:
     * POST /api/questions/from-cv
     */
    @PostMapping("/from-cv")
    public ResponseEntity<List<Question>> generateFromCv(
            @RequestBody CvQuestionsRequest request) {

        List<Question> questions = questionService.generateFromCv(
                request.getCvText(),
                request.getTechCount(),
                request.getSoftCount()
        );

        return ResponseEntity.ok(questions);
    }
}
