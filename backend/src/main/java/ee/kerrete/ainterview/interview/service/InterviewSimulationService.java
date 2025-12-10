package ee.kerrete.ainterview.interview.service;

import ee.kerrete.ainterview.interview.dto.InterviewQuestionDto;
import ee.kerrete.ainterview.interview.dto.InterviewSimulationRequest;
import ee.kerrete.ainterview.interview.dto.InterviewSimulationResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Deterministic, non-persistent simulator for Ghost Interviewer LITE.
 * <p>
 * This service intentionally does not call any external AI or the database.
 */
@Service
public class InterviewSimulationService {

    public InterviewSimulationResponse simulate(InterviewSimulationRequest request) {
        String normalizedRole = request.roleTitle() != null
                ? request.roleTitle().toLowerCase(Locale.ROOT)
                : "";

        List<InterviewQuestionDto> questions = new ArrayList<>();
        if (normalizedRole.contains("backend")) {
            questions.addAll(backendQuestions(request));
        } else if (normalizedRole.contains("frontend")) {
            questions.addAll(frontendQuestions(request));
        } else {
            questions.addAll(genericQuestions(request));
        }

        String summary = buildSummary(request, questions.size());
        List<String> nextSteps = buildNextSteps(request);

        return new InterviewSimulationResponse(
                UUID.randomUUID().toString(),
                request.companyName(),
                request.roleTitle(),
                request.seniority(),
                questions,
                summary,
                nextSteps
        );
    }

    private List<InterviewQuestionDto> backendQuestions(InterviewSimulationRequest request) {
        String company = request.companyName();
        String role = request.roleTitle();
        String seniority = defaultSeniority(request.seniority());

        List<InterviewQuestionDto> list = new ArrayList<>();
        list.add(InterviewQuestionDto.builder()
                .id("backend-architecture")
                .category("Architecture")
                .difficulty(seniorityDifficulty(seniority))
                .question("How would you design a resilient microservice handling high-volume traffic for "
                        + company + " as a " + role + "?")
                .answer("I explain a simple, production-minded design using stateless services, a database per service, "
                        + "and observability baked in from day one.")
                .suggestedAnswer("Describe how you'd split domains into services, choose communication patterns "
                        + "(sync vs async), manage data consistency (sagas/outbox), and add metrics, logs, and tracing.")
                .build());

        list.add(InterviewQuestionDto.builder()
                .id("backend-spring")
                .category("Spring Boot")
                .difficulty(seniorityDifficulty(seniority))
                .question("In a Spring Boot REST API, how do you structure layers to keep business logic maintainable?")
                .answer("I outline a clean layering: controller → service → repository, with DTOs at the boundary "
                        + "and clear separation of concerns.")
                .suggestedAnswer("Talk about why you avoid bloated controllers, how you encapsulate rules in services, "
                        + "and how you keep persistence concerns behind repositories or ports.")
                .build());

        list.add(InterviewQuestionDto.builder()
                .id("backend-resilience")
                .category("Reliability")
                .difficulty(seniorityDifficulty(seniority))
                .question("Tell me about a time you improved the reliability or performance of a backend system.")
                .answer("I share a concise story where I identified a bottleneck, collaborated with the team, "
                        + "and shipped a measurable improvement.")
                .suggestedAnswer("Use a STAR story focused on a slow or flaky system you helped stabilize, including "
                        + "metrics, trade-offs, and what you learned.")
                .build());
        return list;
    }

    private List<InterviewQuestionDto> frontendQuestions(InterviewSimulationRequest request) {
        String company = request.companyName();
        String role = request.roleTitle();
        String seniority = defaultSeniority(request.seniority());

        List<InterviewQuestionDto> list = new ArrayList<>();
        list.add(InterviewQuestionDto.builder()
                .id("frontend-ux")
                .category("UX & Product")
                .difficulty(seniorityDifficulty(seniority))
                .question("How would you collaborate with design and product to ship a new feature for "
                        + company + " as a " + role + "?")
                .answer("I explain how I clarify the problem, explore UX trade-offs, and keep scope small for a fast, "
                        + "high-quality first release.")
                .suggestedAnswer("Mention discovery with product/design, quick prototypes, accessibility, "
                        + "and how you validate the solution after launch.")
                .build());

        list.add(InterviewQuestionDto.builder()
                .id("frontend-performance")
                .category("Performance")
                .difficulty(seniorityDifficulty(seniority))
                .question("What techniques do you use in modern frontend (e.g. Angular) to keep UI fast and responsive?")
                .answer("I talk about change detection, code splitting, caching, and how I measure performance "
                        + "instead of guessing.")
                .suggestedAnswer("Discuss lazy loading, memoization, avoiding unnecessary DOM work, and using tools like "
                        + "Lighthouse or browser devtools to spot regressions.")
                .build());

        list.add(InterviewQuestionDto.builder()
                .id("frontend-quality")
                .category("Quality")
                .difficulty(seniorityDifficulty(seniority))
                .question("Describe how you keep a large frontend codebase maintainable over time.")
                .answer("I share practices like clear module boundaries, reusable components, and a pragmatic testing strategy.")
                .suggestedAnswer("Highlight patterns (smart/dumb components, feature modules), type safety, "
                        + "and how you avoid regressions with tests and linting.")
                .build());
        return list;
    }

    private List<InterviewQuestionDto> genericQuestions(InterviewSimulationRequest request) {
        String company = request.companyName();
        String role = request.roleTitle();
        String seniority = defaultSeniority(request.seniority());

        List<InterviewQuestionDto> list = new ArrayList<>();
        list.add(InterviewQuestionDto.builder()
                .id("behavioral-impact")
                .category("Behavioral")
                .difficulty(seniorityDifficulty(seniority))
                .question("Tell me about a time you had outsized impact in your role at " + company + ".")
                .answer("I give a sharp story that shows ownership, collaboration, and clear business impact.")
                .suggestedAnswer("Use a STAR story with a specific challenge, what you owned, and the measurable result.")
                .build());

        list.add(InterviewQuestionDto.builder()
                .id("behavioral-feedback")
                .category("Behavioral")
                .difficulty(seniorityDifficulty(seniority))
                .question("Describe a piece of tough feedback you received and how it changed your approach.")
                .answer("I focus on a real example where I adjusted my behavior and improved outcomes for the team.")
                .suggestedAnswer("Pick feedback that shows humility, growth, and better collaboration or delivery afterward.")
                .build());

        list.add(InterviewQuestionDto.builder()
                .id("behavioral-onboarding")
                .category("Behavioral")
                .difficulty(seniorityDifficulty(seniority))
                .question("How would you approach your first 90 days as a " + role + " at " + company + "?")
                .answer("I describe a simple plan: learn context, build relationships, and ship a meaningful early win.")
                .suggestedAnswer("Outline a 30/60/90-style plan with discovery, alignment, and one or two concrete wins.")
                .build());
        return list;
    }

    private String buildSummary(InterviewSimulationRequest request, int questionCount) {
        String company = request.companyName();
        String role = request.roleTitle();
        String seniority = defaultSeniority(request.seniority());

        return "Generated a short Ghost Interviewer LITE session for a "
                + seniority.toLowerCase(Locale.ROOT) + " "
                + role + " role at " + company + " with "
                + questionCount + " focused questions and model answers.";
    }

    private List<String> buildNextSteps(InterviewSimulationRequest request) {
        String role = request.roleTitle();
        String company = request.companyName();

        List<String> steps = new ArrayList<>();
        steps.add("Practice answering each question out loud and refine your stories to be concise and outcome-focused.");
        steps.add("Tailor at least one story specifically to " + company + " and how you would operate there as a " + role + ".");
        steps.add("Capture your best answers in notes so you can re-use and adapt them across interviews.");
        return steps;
    }

    private String defaultSeniority(String seniority) {
        if (seniority == null || seniority.isBlank()) {
            return "Mid";
        }
        return seniority;
    }

    private String seniorityDifficulty(String seniority) {
        String normalized = seniority.toLowerCase(Locale.ROOT);
        if (normalized.contains("junior")) {
            return "Easy";
        }
        if (normalized.contains("senior")) {
            return "Hard";
        }
        return "Medium";
    }
}


