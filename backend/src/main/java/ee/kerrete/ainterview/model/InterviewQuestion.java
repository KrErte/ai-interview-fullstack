package ee.kerrete.ainterview.model;

import java.util.List;

public class InterviewQuestion {
    private String text;
    private String difficulty;
    private List<String> tags;

    public InterviewQuestion() {
    }

    public InterviewQuestion(String text, String difficulty, List<String> tags) {
        this.text = text;
        this.difficulty = difficulty;
        this.tags = tags;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
