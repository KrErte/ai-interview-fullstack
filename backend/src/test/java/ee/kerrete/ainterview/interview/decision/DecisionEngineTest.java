package ee.kerrete.ainterview.interview.decision;

import org.junit.jupiter.api.Test;

import java.util.NavigableSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionEngineTest {

    private final DecisionEngine engine = new DecisionEngine();

    private InterviewState baseState() {
        InterviewState s = new InterviewState();
        s.setCurrentStyle(InterviewerStyle.HR);
        s.setCurrentDimension("d1");
        s.getAvailableQuestions()
            .computeIfAbsent("d1", k -> new java.util.TreeMap<>())
            .computeIfAbsent(InterviewerStyle.HR, k -> new TreeSet<>())
            .addAll(java.util.List.of("q1", "q2"));
        s.getAvailableQuestions()
            .get("d1")
            .computeIfAbsent(InterviewerStyle.TECH, k -> new TreeSet<>())
            .addAll(java.util.List.of("q3", "q4"));
        s.getAvailableQuestions()
            .computeIfAbsent("d2", k -> new java.util.TreeMap<>())
            .computeIfAbsent(InterviewerStyle.HR, k -> new TreeSet<>())
            .addAll(java.util.List.of("q5"));
        return s;
    }

    @Test
    void terminatesOnMaxTotalQuestions() {
        InterviewState s = baseState();
        s.getAskedQuestionIds().addAll(java.util.Collections.nCopies(30, "x"));
        var res = engine.decideNextQuestion(s);
        assertThat(res.isEndInterview()).isTrue();
    }

    @Test
    void styleExhaustedSwitchesStyle() {
        InterviewState s = baseState();
        s.getAvailableQuestions().get("d1").get(InterviewerStyle.HR).clear();
        var res = engine.decideNextQuestion(s);
        assertThat(res.isEndInterview()).isFalse();
        assertThat(res.getState().getCurrentStyle()).isEqualTo(InterviewerStyle.TECH);
        assertThat(res.getQuestionId()).isEqualTo("q3");
    }

    @Test
    void probeDepthTechRequires3BeforeSwitch() {
        InterviewState s = baseState();
        s.setCurrentStyle(InterviewerStyle.TECH);
        s.setProbeCount(2); // below threshold 3
        // simulate trend improving by populating scores
        s.dimensionState("d1").addScore(0.2);
        s.dimensionState("d1").addScore(0.3);
        s.dimensionState("d1").addScore(0.4);

        var res = engine.decideNextQuestion(s);
        assertThat(res.isEndInterview()).isFalse();
        assertThat(res.getState().getCurrentStyle()).isEqualTo(InterviewerStyle.TECH); // no switch yet
        assertThat(res.getQuestionId()).isEqualTo("q1"); // smallest available
    }

    @Test
    void exhaustionFallbackSwitchesDimension() {
        InterviewState s = baseState();
        // exhaust d1 all styles
        s.getAvailableQuestions().get("d1").values().forEach(NavigableSet::clear);
        var res = engine.decideNextQuestion(s);
        assertThat(res.isEndInterview()).isFalse();
        assertThat(res.getState().getCurrentDimension()).isEqualTo("d2");
        assertThat(res.getQuestionId()).isEqualTo("q5");
    }

    @Test
    void terminationOnExhaustion() {
        InterviewState s = new InterviewState();
        s.setCurrentDimension("d1");
        s.setCurrentStyle(InterviewerStyle.HR);
        s.getAvailableQuestions().put("d1", new java.util.TreeMap<>());
        var res = engine.decideNextQuestion(s);
        assertThat(res.isEndInterview()).isTrue();
    }
}

