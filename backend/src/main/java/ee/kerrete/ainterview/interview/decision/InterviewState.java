package ee.kerrete.ainterview.interview.decision;

import java.util.Deque;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class InterviewState {

    private InterviewerStyle currentStyle;
    private String currentDimension;
    private int probeCount;
    private final CountingSet askedQuestionIds = new CountingSet();
    private final Map<String, Map<InterviewerStyle, NavigableSet<String>>> availableQuestions = new TreeMap<>();
    private final Map<String, DimensionState> dimensionStates = new TreeMap<>();
    private int totalQuestionsAsked;

    public InterviewerStyle getCurrentStyle() {
        return currentStyle;
    }

    public void setCurrentStyle(InterviewerStyle currentStyle) {
        this.currentStyle = currentStyle;
    }

    public String getCurrentDimension() {
        return currentDimension;
    }

    public void setCurrentDimension(String currentDimension) {
        this.currentDimension = currentDimension;
    }

    public int getProbeCount() {
        return probeCount;
    }

    public void setProbeCount(int probeCount) {
        this.probeCount = probeCount;
    }

    public void incrementProbeCount() {
        this.probeCount++;
    }

    public Set<String> getAskedQuestionIds() {
        return askedQuestionIds;
    }

    public Map<String, Map<InterviewerStyle, NavigableSet<String>>> getAvailableQuestions() {
        return availableQuestions;
    }

    public Map<String, DimensionState> getDimensionStates() {
        return dimensionStates;
    }

    public int getTotalQuestionsAsked() {
        return totalQuestionsAsked;
    }

    public void setTotalQuestionsAsked(int totalQuestionsAsked) {
        this.totalQuestionsAsked = totalQuestionsAsked;
    }

    public void addAvailableQuestion(String dimension, InterviewerStyle style, String questionId) {
        availableQuestions
            .computeIfAbsent(dimension, k -> new TreeMap<>())
            .computeIfAbsent(style, k -> new TreeSet<>())
            .add(questionId);
    }

    public NavigableSet<String> availableForCurrent() {
        return availableQuestions
            .getOrDefault(currentDimension, Map.of())
            .getOrDefault(currentStyle, new TreeSet<>());
    }

    public DimensionState dimensionState(String dimension) {
        return dimensionStates.computeIfAbsent(dimension, d -> new DimensionState());
    }

    public Deque<Double> last3Scores(String dimension) {
        return dimensionState(dimension).getLast3Scores();
    }

    public int getAskedAdds() {
        return askedQuestionIds.getAddCount();
    }

    private static class CountingSet extends java.util.HashSet<String> {
        private int addCount = 0;

        @Override
        public boolean add(String s) {
            addCount++;
            return super.add(s);
        }

        @Override
        public boolean addAll(java.util.Collection<? extends String> c) {
            addCount += c.size();
            return super.addAll(c);
        }

        public int getAddCount() {
            return addCount;
        }
    }
}

