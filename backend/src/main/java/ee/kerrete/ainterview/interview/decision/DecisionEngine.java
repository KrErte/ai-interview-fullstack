package ee.kerrete.ainterview.interview.decision;

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class DecisionEngine {

    private static final int MAX_PROBES_PER_DIMENSION_PER_STYLE = 3;
    private static final int MAX_TOTAL_PROBES_PER_DIMENSION = 9;
    private static final int MAX_TOTAL_QUESTIONS = 30;
    private static final double TREND_THRESHOLD = 0.05;

    private static final Map<InterviewerStyle, Integer> STYLE_PROBE_DEPTH = Map.of(
        InterviewerStyle.HR, 2,
        InterviewerStyle.TEAM_LEAD, 2,
        InterviewerStyle.TECH, 3
    );

    private static final InterviewerStyle[] STYLE_ORDER = {
        InterviewerStyle.HR, InterviewerStyle.TECH, InterviewerStyle.TEAM_LEAD
    };

    public DecisionEngineResult decideNextQuestion(InterviewState state) {
        if (state == null) throw new IllegalArgumentException("state required");

        int effectiveAsked = Math.max(state.getTotalQuestionsAsked(),
            Math.max(state.getAskedQuestionIds().size(), state.getAskedAdds()));
        state.setTotalQuestionsAsked(effectiveAsked);

        // R1 terminate
        if (state.getTotalQuestionsAsked() >= MAX_TOTAL_QUESTIONS || allExhausted(state)) {
            return DecisionEngineResult.builder().endInterview(true).state(state).build();
        }

        // Ensure current dimension/style valid
        ensureCurrentDimension(state);
        ensureCurrentStyle(state);

        NavigableSet<String> currentSet = state.availableForCurrent();
        boolean styleExhausted = currentSet.isEmpty();
        boolean dimensionExhausted = dimensionExhausted(state, state.getCurrentDimension());

        Trend trend = computeTrend(state);

        // R2 style exhausted
        if (styleExhausted) {
            if (switchToNextStyleSameDimension(state)) {
                resetProbe(state);
                return pick(state);
            }
            if (switchToNextDimension(state)) {
                resetProbe(state);
                return pick(state);
            }
            return DecisionEngineResult.builder().endInterview(true).state(state).build();
        }

        // R3 max probes per style
        if (state.getProbeCount() >= MAX_PROBES_PER_DIMENSION_PER_STYLE) {
            if (switchToNextStyleSameDimension(state)) {
                resetProbe(state);
                return pick(state);
            }
            if (state.dimensionState(state.getCurrentDimension()).getTotalProbes() >= MAX_TOTAL_PROBES_PER_DIMENSION || dimensionExhausted) {
                if (switchToLowestProbeDimension(state)) {
                    resetProbe(state);
                    return pick(state);
                }
                return DecisionEngineResult.builder().endInterview(true).state(state).build();
            }
        }

        // R4 trend declining -> HR
        if (trend == Trend.DECLINING
            && state.getCurrentStyle() != InterviewerStyle.HR
            && hasAvailable(state, state.getCurrentDimension(), InterviewerStyle.HR)
            && state.dimensionState(state.getCurrentDimension()).getTotalProbes() < MAX_TOTAL_PROBES_PER_DIMENSION) {
            state.setCurrentStyle(InterviewerStyle.HR);
            resetProbe(state);
            return pick(state);
        }

        // R5a flat -> TEAM_LEAD with depth guard
        if (trend == Trend.FLAT
            && state.getProbeCount() >= STYLE_PROBE_DEPTH.get(state.getCurrentStyle())
            && state.getCurrentStyle() != InterviewerStyle.TEAM_LEAD
            && hasAvailable(state, state.getCurrentDimension(), InterviewerStyle.TEAM_LEAD)
            && state.dimensionState(state.getCurrentDimension()).getTotalProbes() < MAX_TOTAL_PROBES_PER_DIMENSION) {
            state.setCurrentStyle(InterviewerStyle.TEAM_LEAD);
            resetProbe(state);
            return pick(state);
        }

        // R5b improving -> TECH with depth guard
        if (trend == Trend.IMPROVING
            && state.getProbeCount() >= STYLE_PROBE_DEPTH.get(state.getCurrentStyle())
            && state.getCurrentStyle() != InterviewerStyle.TECH
            && hasAvailable(state, state.getCurrentDimension(), InterviewerStyle.TECH)
            && state.dimensionState(state.getCurrentDimension()).getTotalProbes() < MAX_TOTAL_PROBES_PER_DIMENSION) {
            state.setCurrentStyle(InterviewerStyle.TECH);
            resetProbe(state);
            return pick(state);
        }

        // R6 dimension exhausted
        if (state.dimensionState(state.getCurrentDimension()).getTotalProbes() >= MAX_TOTAL_PROBES_PER_DIMENSION || dimensionExhausted) {
            if (switchToLowestProbeDimension(state)) {
                resetProbe(state);
                return pick(state);
            }
            return DecisionEngineResult.builder().endInterview(true).state(state).build();
        }

        // R7 default continue
        return pick(state);
    }

    private DecisionEngineResult pick(InterviewState state) {
        String questionId = pickSmallestAcrossStyles(state);
        if (questionId == null) {
            return DecisionEngineResult.builder().endInterview(true).state(state).build();
        }
        removeFromAvailable(state, questionId);
        state.getAskedQuestionIds().add(questionId);
        state.incrementProbeCount();
        state.dimensionState(state.getCurrentDimension()).incrementTotalProbes();
        state.setTotalQuestionsAsked(Math.max(state.getTotalQuestionsAsked(), state.getAskedAdds()));
        return DecisionEngineResult.builder()
            .endInterview(false)
            .questionId(questionId)
            .state(state)
            .build();
    }

    private String pickSmallestAcrossStyles(InterviewState state) {
        Map<InterviewerStyle, NavigableSet<String>> byStyle = state.getAvailableQuestions()
            .getOrDefault(state.getCurrentDimension(), Map.of());
        return byStyle.values().stream()
            .filter(s -> !s.isEmpty())
            .flatMap(Set::stream)
            .min(String::compareTo)
            .orElse(null);
    }

    private void removeFromAvailable(InterviewState state, String questionId) {
        Map<InterviewerStyle, NavigableSet<String>> byStyle = state.getAvailableQuestions()
            .getOrDefault(state.getCurrentDimension(), Map.of());
        byStyle.values().forEach(set -> set.remove(questionId));
    }

    private boolean allExhausted(InterviewState state) {
        return state.getAvailableQuestions().values().stream()
            .flatMap(m -> m.values().stream())
            .allMatch(Set::isEmpty);
    }

    private boolean dimensionExhausted(InterviewState state, String dimension) {
        Map<InterviewerStyle, NavigableSet<String>> byStyle = state.getAvailableQuestions().getOrDefault(dimension, Map.of());
        return byStyle.values().stream().allMatch(Set::isEmpty);
    }

    private boolean hasAvailable(InterviewState state, String dimension, InterviewerStyle style) {
        return !state.getAvailableQuestions()
            .getOrDefault(dimension, Map.of())
            .getOrDefault(style, new TreeSet<>())
            .isEmpty();
    }

    private void ensureCurrentDimension(InterviewState state) {
        if (state.getCurrentDimension() != null) return;
        if (state.getAvailableQuestions().isEmpty()) {
            throw new IllegalStateException("No dimensions configured");
        }
        state.setCurrentDimension(state.getAvailableQuestions().keySet().iterator().next());
    }

    private void ensureCurrentStyle(InterviewState state) {
        if (state.getCurrentStyle() != null) return;
        state.setCurrentStyle(InterviewerStyle.HR);
    }

    private boolean switchToNextStyleSameDimension(InterviewState state) {
        for (int i = 1; i <= 3; i++) {
            InterviewerStyle next = STYLE_ORDER[(index(state.getCurrentStyle()) + i) % 3];
            if (hasAvailable(state, state.getCurrentDimension(), next)) {
                state.setCurrentStyle(next);
                return true;
            }
        }
        return false;
    }

    private boolean switchToNextDimension(InterviewState state) {
        var dims = state.getAvailableQuestions().keySet().stream().sorted().toList();
        int idx = dims.indexOf(state.getCurrentDimension());
        for (int i = 1; i <= dims.size(); i++) {
            String nextDim = dims.get((idx + i) % dims.size());
            if (!dimensionExhausted(state, nextDim) && hasAvailable(state, nextDim, InterviewerStyle.HR)) {
                state.setCurrentDimension(nextDim);
                state.setCurrentStyle(InterviewerStyle.HR);
                return true;
            }
        }
        return false;
    }

    private boolean switchToLowestProbeDimension(InterviewState state) {
        Optional<Map.Entry<String, DimensionState>> target = state.getDimensionStates().entrySet().stream()
            .filter(e -> hasAvailable(state, e.getKey(), InterviewerStyle.HR))
            .min(Comparator.<Map.Entry<String, DimensionState>>comparingInt(e -> e.getValue().getTotalProbes())
                .thenComparing(Map.Entry::getKey));
        if (target.isPresent()) {
            state.setCurrentDimension(target.get().getKey());
            state.setCurrentStyle(InterviewerStyle.HR);
            return true;
        }
        return false;
    }

    private void resetProbe(InterviewState state) {
        state.setProbeCount(0);
    }

    private Trend computeTrend(InterviewState state) {
        var dq = state.last3Scores(state.getCurrentDimension());
        if (dq.size() < 3) return Trend.MIXED;
        double s1 = dq.peekFirst();
        double s2 = dq.stream().skip(1).findFirst().orElse(0.0);
        double s3 = dq.peekLast();
        double d1 = s2 - s1;
        double d2 = s3 - s2;
        if (d1 > TREND_THRESHOLD && d2 > TREND_THRESHOLD) return Trend.IMPROVING;
        if (d1 < -TREND_THRESHOLD && d2 < -TREND_THRESHOLD) return Trend.DECLINING;
        if (Math.abs(d1) <= TREND_THRESHOLD && Math.abs(d2) <= TREND_THRESHOLD) return Trend.FLAT;
        return Trend.MIXED;
    }

    private int index(InterviewerStyle s) {
        for (int i = 0; i < STYLE_ORDER.length; i++) {
            if (STYLE_ORDER[i] == s) return i;
        }
        return 0;
    }
}

