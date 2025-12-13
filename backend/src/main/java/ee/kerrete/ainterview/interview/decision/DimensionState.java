package ee.kerrete.ainterview.interview.decision;

import java.util.ArrayDeque;
import java.util.Deque;

public class DimensionState {
    private final Deque<Double> last3Scores = new ArrayDeque<>();
    private int totalProbes;

    public Deque<Double> getLast3Scores() {
        return last3Scores;
    }

    public int getTotalProbes() {
        return totalProbes;
    }

    public void incrementTotalProbes() {
        totalProbes++;
    }

    public void addScore(double score) {
        if (last3Scores.size() == 3) {
            last3Scores.removeFirst();
        }
        last3Scores.addLast(score);
    }
}

