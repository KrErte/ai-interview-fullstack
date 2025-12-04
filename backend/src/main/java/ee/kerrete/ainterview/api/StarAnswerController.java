package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.StarAnswerRequest;
import ee.kerrete.ainterview.dto.StarAnswerResponse;
import ee.kerrete.ainterview.service.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/star")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StarAnswerController {

    private final OpenAiClient openAIClient;

    @PostMapping
    public ResponseEntity<StarAnswerResponse> generate(@RequestBody StarAnswerRequest req) {

        String prompt = """
                You are a senior developer helping a candidate prepare for a job interview.
                Use STAR format (Situation, Task, Action, Result).
                Estonian language.

                CV:
                %s

                Job description:
                %s

                Interview question:
                %s

                Generate a strong STAR answer:
                - Realistic (not too perfect),
                - Based on CV,
                - Estonian,
                - Max 8 sentences,
                - Good interview flow.
                """.formatted(req.getCvText(), req.getJobDescription(), req.getQuestion());

        String response = openAIClient.complete(prompt);

        return ResponseEntity.ok(
                StarAnswerResponse.builder()
                        .answer(response.trim())
                        .build()
        );
    }
}
