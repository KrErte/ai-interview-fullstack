package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.CvQuestionsRequest;
import ee.kerrete.ainterview.model.Question;
import ee.kerrete.ainterview.service.QuestionService;
import jakarta.validation.Valid;
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
            @Valid @RequestBody CvQuestionsRequest request) {

        List<Question> questions = questionService.generateFromCv(
                request.getCvText(),
                request.getTechCount(),
                request.getSoftCount()
        );

        return ResponseEntity.ok(questions);
    }
}
