package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hoiab Job Matcheri analüüside ajalugu mälus.
 * Key: email, Value: analüüside nimekiri.
 */
@Service
public class JobAnalysisStatsService {

    private final ConcurrentHashMap<String, List<JobAnalysisRecord>> historyByEmail =
            new ConcurrentHashMap<>();

    /**
     * Logi iga analüüs pärast JobAnalysisService.analyze() väljakutset.
     */
    public void recordAnalysis(JobAnalysisRequest request, JobAnalysisResponse response) {
        if (request == null) {
            return;
        }

        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            // Kui email puudub, ei seo kasutajaga
            return;
        }

        Double matchScore = response != null ? response.getMatchScore() : null;
        String summary = response != null ? response.getSummary() : null;

        JobAnalysisRecord record = new JobAnalysisRecord(
                email,
                matchScore,
                summary,
                OffsetDateTime.now()
        );

        historyByEmail
                .computeIfAbsent(email, k -> new ArrayList<>())
                .add(record);
    }

    /**
     * Kõigi analüüside koguarv (kõigi kasutajate peale).
     */
    public int getTotalAnalyses() {
        return historyByEmail.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Antud emailiga tehtud analüüside arv.
     */
    public int getTotalAnalysesFor(String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        return historyByEmail
                .getOrDefault(email, Collections.emptyList())
                .size();
    }

    /**
     * Viimane analüüs antud emaili jaoks (või null, kui puudub).
     */
    public JobAnalysisRecord getLastFor(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        List<JobAnalysisRecord> list = historyByEmail.get(email);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /**
     * Sisemine mudel ühe analüüsi kirjeldamiseks.
     */
    public static class JobAnalysisRecord {
        private final String email;
        private final Double matchScore;
        private final String summary;
        private final OffsetDateTime timestamp;

        public JobAnalysisRecord(String email,
                                 Double matchScore,
                                 String summary,
                                 OffsetDateTime timestamp) {
            this.email = email;
            this.matchScore = matchScore;
            this.summary = summary;
            this.timestamp = timestamp;
        }

        public String getEmail() {
            return email;
        }

        public Double getMatchScore() {
            return matchScore;
        }

        public String getSummary() {
            return summary;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }
    }
}
