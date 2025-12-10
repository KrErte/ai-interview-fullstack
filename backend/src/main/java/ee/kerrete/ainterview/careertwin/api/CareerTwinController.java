package ee.kerrete.ainterview.careertwin.api;

import ee.kerrete.ainterview.careertwin.dto.CareerTwinAppendRequest;
import ee.kerrete.ainterview.careertwin.dto.CareerTwinInsightResponse;
import ee.kerrete.ainterview.careertwin.service.CareerTwinService;
import ee.kerrete.ainterview.model.UserMemoryEntry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/career-twin")
@RequiredArgsConstructor
public class CareerTwinController {

    private final CareerTwinService careerTwinService;

    @PostMapping("/append")
    @ResponseStatus(HttpStatus.CREATED)
    public UserMemoryEntry append(@Valid @RequestBody CareerTwinAppendRequest request) {
        return careerTwinService.appendEntry(request);
    }

    @GetMapping("/insights")
    public CareerTwinInsightResponse insights() {
        return careerTwinService.getInsights();
    }
}

