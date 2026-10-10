package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.CatalogModels.GeneralStage;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.aireadiness.CatalogModels.ProfessionTask;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Laeb AI valmisoleku testi sisu ja kontrollib käivitusel, et andmed on kooskõlas
 * (ülesannete osakaalud = 100%, automatiseeritavus 0..1, unikaalsed id-d).
 */
@Component
public class AiReadinessCatalog {

    private final List<Profession> professions;
    private final Map<String, Profession> professionsById;
    private final List<Question> questions;
    private final List<GeneralStage> generalRoadmap;

    @Autowired
    public AiReadinessCatalog(ObjectMapper objectMapper) {
        this(
                read(objectMapper, "ai-readiness/professions.json", new TypeReference<>() { }),
                read(objectMapper, "ai-readiness/questions.json", new TypeReference<>() { }),
                read(objectMapper, "ai-readiness/general-roadmap.json", new TypeReference<>() { })
        );
    }

    AiReadinessCatalog(List<Profession> professions, List<Question> questions, List<GeneralStage> generalRoadmap) {
        this.professions = List.copyOf(professions);
        this.questions = List.copyOf(questions);
        this.generalRoadmap = List.copyOf(generalRoadmap);
        this.professionsById = new LinkedHashMap<>();
        for (Profession p : professions) {
            validate(p);
            if (professionsById.put(p.id(), p) != null) {
                throw new IllegalStateException("Duplicate profession id: " + p.id());
            }
        }
    }

    public List<Profession> professions() {
        return professions;
    }

    public Optional<Profession> profession(String id) {
        return Optional.ofNullable(professionsById.get(id));
    }

    public List<Question> questions() {
        return questions;
    }

    public List<GeneralStage> generalRoadmap() {
        return generalRoadmap;
    }

    private static void validate(Profession p) {
        int total = p.tasks().stream().mapToInt(ProfessionTask::share).sum();
        if (total != 100) {
            throw new IllegalStateException("Task shares of '" + p.id() + "' sum to " + total + ", expected 100");
        }
        for (ProfessionTask t : p.tasks()) {
            if (t.automation() < 0 || t.automation() > 1) {
                throw new IllegalStateException("Automation of '" + t.name() + "' must be within 0..1");
            }
        }
    }

    private static <T> T read(ObjectMapper mapper, String path, TypeReference<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + path, e);
        }
    }
}
