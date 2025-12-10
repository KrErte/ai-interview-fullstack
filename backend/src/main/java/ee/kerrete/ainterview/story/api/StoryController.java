package ee.kerrete.ainterview.story.api;

import ee.kerrete.ainterview.story.dto.BehavioralStoryRequest;
import ee.kerrete.ainterview.story.dto.BehavioralStoryResponse;
import ee.kerrete.ainterview.story.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @GetMapping
    public List<BehavioralStoryResponse> list() {
        return storyService.listStories();
    }

    @GetMapping("/{id}")
    public BehavioralStoryResponse get(@PathVariable Long id) {
        return storyService.getStory(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BehavioralStoryResponse create(@Valid @RequestBody BehavioralStoryRequest request) {
        return storyService.createStory(request);
    }

    @PutMapping("/{id}")
    public BehavioralStoryResponse update(@PathVariable Long id,
                                          @Valid @RequestBody BehavioralStoryRequest request) {
        return storyService.updateStory(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        storyService.deleteStory(id);
    }
}

