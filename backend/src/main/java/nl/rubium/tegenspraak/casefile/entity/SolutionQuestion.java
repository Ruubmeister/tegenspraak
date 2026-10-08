package nl.rubium.tegenspraak.casefile.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A multiple choice question; {@code answer} is the id of the correct option. */
public record SolutionQuestion(String id, String question, List<SolutionOption> options, String answer) {

    public SolutionQuestion {
        if (id == null || id.isBlank() || question == null || question.isBlank()) {
            throw new IllegalArgumentException("A solution question needs an id and a question");
        }
        if (options == null || options.size() < 2) {
            throw new IllegalArgumentException("Question '" + id + "' needs at least two options");
        }
        options = List.copyOf(options);
        requireUniqueOptionIds(id, options);
        if (!hasOption(options, answer)) {
            throw new IllegalArgumentException("Answer of question '" + id + "' is not one of its options");
        }
    }

    public boolean hasOption(String optionId) {
        return hasOption(options, optionId);
    }

    public boolean isAnsweredCorrectly(String optionId) {
        return answer.equals(optionId);
    }

    private static boolean hasOption(List<SolutionOption> options, String optionId) {
        return options.stream().anyMatch(option -> option.id().equals(optionId));
    }

    private static void requireUniqueOptionIds(String questionId, List<SolutionOption> options) {
        Set<String> seenIds = new HashSet<>();
        for (SolutionOption option : options) {
            if (!seenIds.add(option.id())) {
                throw new IllegalArgumentException(
                        "Duplicate option id '" + option.id() + "' in question '" + questionId + "'");
            }
        }
    }
}
