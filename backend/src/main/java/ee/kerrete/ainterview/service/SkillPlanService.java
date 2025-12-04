package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.SkillPlanDay;
import ee.kerrete.ainterview.dto.SkillPlanRequest;
import ee.kerrete.ainterview.dto.SkillPlanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillPlanService {

    private static final int DEFAULT_PLAN_LENGTH_DAYS = 21;

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public SkillPlanResponse buildPlan(SkillPlanRequest request) {
        try {
            // GPT versioon
            String json = openAiClient.generateSkillBoosterPlan(
                    request.getJobMatcherSummary(),
                    request.getFocusSkills()
            );

            return objectMapper.readValue(json, SkillPlanResponse.class);
        } catch (Exception e) {
            // Fallback – töötab ka ilma GPT-ta
            return buildFallbackPlan(request);
        }
    }

    private SkillPlanResponse buildFallbackPlan(SkillPlanRequest request) {
        List<String> skills = request.getFocusSkills();
        if (skills == null || skills.isEmpty()) {
            skills = List.of("Problem solving", "Software fundamentals");
        }

        List<SkillPlanDay> days = new ArrayList<>();
        int dayNumber = 1;
        int skillIndex = 0;

        while (dayNumber <= DEFAULT_PLAN_LENGTH_DAYS) {
            String skill = skills.get(skillIndex % skills.size());

            SkillPlanDay day = SkillPlanDay.builder()
                    .dayNumber(dayNumber)
                    .title(phaseTitle(dayNumber, skill))
                    .description(phaseDescription(dayNumber, skill, request.getJobMatcherSummary()))
                    .practiceTask(examplePractice(dayNumber, skill))
                    .build();

            days.add(day);
            dayNumber++;
            skillIndex++;
        }

        return SkillPlanResponse.builder()
                .overallGoal("Fallback-plaan: keskendutakse oskustele " + String.join(", ", skills))
                .days(days)
                .build();
    }

    private String phaseTitle(int dayNumber, String skill) {
        if (dayNumber <= 7) return "Alused – " + skill;
        if (dayNumber <= 14) return "Praktiline projekt – " + skill;
        return "Intervjuu valmistumine – " + skill;
    }

    private String phaseDescription(int dayNumber, String skill, String summary) {
        String phase = (dayNumber <= 7)
                ? "Fookus baasteadmistel."
                : (dayNumber <= 14)
                ? "Rakenda praktikas väikese projektiga."
                : "Valmistu sellest rääkima intervjuul.";

        String info = summary == null ? "" :
                " (Job Matcheri kokkvõte arvestatud)";

        return phase + info;
    }

    private String examplePractice(int dayNumber, String skill) {
        if (dayNumber <= 7) return "30 min teooriat + mini snippet selle skilliga.";
        if (dayNumber <= 14) return "Ehita väike funktsionaalsus või demo '" + skill + "' kasutades.";
        return "Valmista 1–2 STAR stiilis intervjuu vastust '" + skill + "' kohta.";
    }
}
