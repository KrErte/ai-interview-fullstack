package ee.kerrete.ainterview.dejavu.api;

import ee.kerrete.ainterview.dejavu.dto.DejaVuPredictionRequest;
import ee.kerrete.ainterview.dejavu.dto.DejaVuPredictionResponse;
import ee.kerrete.ainterview.dejavu.service.DejaVuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dejavu")
@RequiredArgsConstructor
public class DejaVuController {

    private final DejaVuService dejaVuService;

    @PostMapping("/predict")
    public DejaVuPredictionResponse predict(@Valid @RequestBody DejaVuPredictionRequest request) {
        return dejaVuService.predict(request);
    }
}

