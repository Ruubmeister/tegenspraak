package nl.rubium.tegenspraak.casefile.entity;

/**
 * An open question the team answers in its own words. {@code answer} describes what a correct answer must contain; the
 * grader uses it as the rubric and the team never sees it.
 */
public record SolutionQuestion(String id, String question, String answer) {

    public SolutionQuestion {
        if (id == null || id.isBlank() || question == null || question.isBlank()) {
            throw new IllegalArgumentException("A solution question needs an id and a question");
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("Question '" + id + "' needs an answer");
        }
    }
}
