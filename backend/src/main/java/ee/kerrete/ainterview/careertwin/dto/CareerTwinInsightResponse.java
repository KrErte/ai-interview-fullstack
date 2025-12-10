package ee.kerrete.ainterview.careertwin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CareerTwinInsightResponse(
        List<String> topKeywords,
        List<MemoryEntry> lastEntries
) {

    public record MemoryEntry(Long id, String entryText, LocalDateTime createdAt) {
    }
}

