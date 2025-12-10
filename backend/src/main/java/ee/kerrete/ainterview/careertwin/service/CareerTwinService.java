package ee.kerrete.ainterview.careertwin.service;

import ee.kerrete.ainterview.careertwin.dto.CareerTwinAppendRequest;
import ee.kerrete.ainterview.careertwin.dto.CareerTwinInsightResponse;
import ee.kerrete.ainterview.model.UserMemoryEntry;
import ee.kerrete.ainterview.repository.UserMemoryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CareerTwinService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "with", "from", "that", "this", "have", "been", "for", "into", "your",
            "about", "when", "what", "where", "will", "would", "should", "could", "their", "they",
            "them", "then", "than", "also", "over", "under", "once", "upon"
    );

    private final UserMemoryEntryRepository userMemoryEntryRepository;

    public UserMemoryEntry appendEntry(CareerTwinAppendRequest request) {
        UserMemoryEntry entry = UserMemoryEntry.builder()
                .entryText(request.entryText())
                .createdAt(LocalDateTime.now())
                .build();
        return userMemoryEntryRepository.save(entry);
    }

    public CareerTwinInsightResponse getInsights() {
        List<UserMemoryEntry> allEntries = userMemoryEntryRepository.findAllByOrderByCreatedAtDesc();
        List<String> keywords = topKeywords(allEntries, 5);
        List<UserMemoryEntry> recent = userMemoryEntryRepository.findTop3ByOrderByCreatedAtDesc();

        List<CareerTwinInsightResponse.MemoryEntry> recentDtos = recent.stream()
                .map(e -> new CareerTwinInsightResponse.MemoryEntry(e.getId(), e.getEntryText(), e.getCreatedAt()))
                .toList();

        return new CareerTwinInsightResponse(keywords, recentDtos);
    }

    private List<String> topKeywords(List<UserMemoryEntry> entries, int limit) {
        Map<String, Long> counts = new HashMap<>();
        for (UserMemoryEntry entry : entries) {
            if (entry.getEntryText() == null) {
                continue;
            }
            Stream.of(entry.getEntryText().toLowerCase(Locale.ENGLISH).split("\\W+"))
                    .filter(token -> token.length() > 3)
                    .filter(token -> !STOP_WORDS.contains(token))
                    .forEach(token -> counts.merge(token, 1L, Long::sum));
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}

