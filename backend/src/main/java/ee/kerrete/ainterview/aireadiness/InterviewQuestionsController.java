package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.JobPages.InterviewQuestion;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI-oskuse intervjuuküsimused ameti kohta intervjuuks valmistumise lehele (/prep). Avalik nagu muu
 * /api/ai-readiness/**; sama sisu, mis ametilehel, O*NET ametitel üldine komplekt nende enda ülesannetest.
 */
@RestController
public class InterviewQuestionsController {

    private final AiReadinessCatalog catalog;
    private final JobPages pages;

    public InterviewQuestionsController(AiReadinessCatalog catalog, JobPages pages) {
        this.catalog = catalog;
        this.pages = pages;
    }

    @GetMapping("/api/ai-readiness/professions/{id}/interview-questions")
    public ResponseEntity<List<InterviewQuestion>> questions(@PathVariable String id) {
        return catalog.profession(id)
                .map(p -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                        .body(pages.interviewQuestionsOrGeneric(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
