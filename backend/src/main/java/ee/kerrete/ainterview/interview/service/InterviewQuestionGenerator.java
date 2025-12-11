package ee.kerrete.ainterview.interview.service;

import ee.kerrete.ainterview.interview.enums.InterviewerStyle;
import ee.kerrete.ainterview.interview.model.InterviewQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewQuestionGenerator {

    /**
     * Deterministic, template-based question generator (no external calls).
     */
    public List<InterviewQuestion> generate(String company, String role, String seniority, InterviewerStyle style, int total) {
        List<InterviewQuestion> questions = new ArrayList<>();
        String companySafe = defaultString(company, "the company");
        String roleSafe = defaultString(role, "the role");
        String senioritySafe = defaultString(seniority, "mid-level");
        InterviewerStyle styleSafe = style != null ? style : InterviewerStyle.MIXED;

        List<String> templates = switch (styleSafe) {
            case HR -> hrTemplates(companySafe, roleSafe, senioritySafe);
            case TECH -> techTemplates(companySafe, roleSafe, senioritySafe);
            case TEAM_LEAD -> teamLeadTemplates(companySafe, roleSafe, senioritySafe);
            case MIXED -> mixedTemplates(companySafe, roleSafe, senioritySafe);
        };

        for (int i = 0; i < total; i++) {
            String base = templates.get(i % templates.size());
            questions.add(InterviewQuestion.builder()
                .question(base)
                .modelAnswerHint(defaultHintFor(styleSafe))
                .build());
        }
        return questions;
    }

    private List<String> hrTemplates(String company, String role, String seniority) {
        return List.of(
            "Tell me about a time you showed ownership in a challenging situation.",
            "How do you adapt your communication when collaborating across teams?",
            "Describe a situation where you had to balance priorities under pressure.",
            "What motivates you to grow in " + role + " at " + company + "?",
            "How do you ensure alignment when working with stakeholders?"
        );
    }

    private List<String> techTemplates(String company, String role, String seniority) {
        return List.of(
            "Design a simple service for " + company + " to support " + role + " users; outline key components.",
            "Walk through a debugging story where you isolated a tricky issue.",
            "Explain a trade-off you made in a system you built as a " + seniority + " engineer.",
            "How would you improve reliability for an API that serves critical workflows?",
            "Describe how you communicate technical risks to non-technical partners."
        );
    }

    private List<String> teamLeadTemplates(String company, String role, String seniority) {
        return List.of(
            "How do you coach peers to raise code quality in " + company + "'s context?",
            "Describe a time you resolved conflict within the team.",
            "How do you balance delivery speed with long-term architecture concerns?",
            "Share an example of leading a cross-team initiative as a " + seniority + " " + role + ".",
            "How do you handle underperformance while keeping morale up?"
        );
    }

    private List<String> mixedTemplates(String company, String role, String seniority) {
        List<String> combined = new ArrayList<>();
        combined.addAll(hrTemplates(company, role, seniority));
        combined.addAll(techTemplates(company, role, seniority));
        combined.addAll(teamLeadTemplates(company, role, seniority));
        return combined;
    }

    private String defaultHintFor(InterviewerStyle style) {
        return switch (style) {
            case HR -> "Be concise, highlight impact, teamwork, and communication.";
            case TECH -> "State assumptions, outline design, mention trade-offs and metrics.";
            case TEAM_LEAD -> "Focus on alignment, conflict resolution, and outcomes.";
            case MIXED -> "Blend technical depth with collaboration and impact.";
        };
    }

    private String defaultString(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}

