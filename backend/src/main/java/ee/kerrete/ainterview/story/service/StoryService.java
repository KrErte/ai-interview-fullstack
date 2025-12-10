package ee.kerrete.ainterview.story.service;

import ee.kerrete.ainterview.model.BehavioralStory;
import ee.kerrete.ainterview.repository.BehavioralStoryRepository;
import ee.kerrete.ainterview.story.dto.BehavioralStoryRequest;
import ee.kerrete.ainterview.story.dto.BehavioralStoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final BehavioralStoryRepository behavioralStoryRepository;

    public List<BehavioralStoryResponse> listStories() {
        return behavioralStoryRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BehavioralStoryResponse getStory(Long id) {
        BehavioralStory story = behavioralStoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        return toResponse(story);
    }

    public BehavioralStoryResponse createStory(BehavioralStoryRequest request) {
        LocalDateTime now = LocalDateTime.now();
        BehavioralStory story = BehavioralStory.builder()
                .title(request.title())
                .situation(request.situation())
                .task(request.task())
                .action(request.action())
                .resultText(request.result())
                .tags(request.tags())
                .createdAt(now)
                .updatedAt(now)
                .build();
        BehavioralStory saved = behavioralStoryRepository.save(story);
        return toResponse(saved);
    }

    public BehavioralStoryResponse updateStory(Long id, BehavioralStoryRequest request) {
        BehavioralStory existing = behavioralStoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));

        existing.setTitle(request.title());
        existing.setSituation(request.situation());
        existing.setTask(request.task());
        existing.setAction(request.action());
        existing.setResultText(request.result());
        existing.setTags(request.tags());
        existing.setUpdatedAt(LocalDateTime.now());

        BehavioralStory saved = behavioralStoryRepository.save(existing);
        return toResponse(saved);
    }

    public void deleteStory(Long id) {
        if (!behavioralStoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found");
        }
        behavioralStoryRepository.deleteById(id);
    }

    private BehavioralStoryResponse toResponse(BehavioralStory story) {
        return new BehavioralStoryResponse(
                story.getId(),
                story.getTitle(),
                story.getSituation(),
                story.getTask(),
                story.getAction(),
                story.getResultText(),
                story.getTags(),
                story.getCreatedAt(),
                story.getUpdatedAt()
        );
    }
}

