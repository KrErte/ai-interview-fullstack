package ee.krerte.aiinterview.api;

import ee.krerte.aiinterview.dto.CvTextResponse;
import ee.krerte.aiinterview.service.CvTextExtractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/cv-text")
@RequiredArgsConstructor
public class CvTextExtractController {

    private final CvTextExtractService cvTextExtractService;

    @PostMapping(
            path = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CvTextResponse> extract(@RequestPart("file") MultipartFile file) {
        String text = cvTextExtractService.extractText(file);
        CvTextResponse response = new CvTextResponse();
        response.setText(text);
        return ResponseEntity.ok(response);
    }
}
