package nl.rubium.tegenspraak.casefile.entity;

import java.util.List;
import java.util.Set;

/**
 * Something a character hides until {@code revealWhen} is met. When {@code requiresEvidence} is set, the team must also
 * have shown the character at least {@code requiredCount} of those mails or files as attachments; until then the
 * character denies it, whatever the team claims. Without a count, all of them are required.
 */
public record CharacterSecret(
        String fact, String revealWhen, List<RequiredEvidence> requiresEvidence, Integer requiredCount) {

    public CharacterSecret {
        if (fact == null || fact.isBlank() || revealWhen == null || revealWhen.isBlank()) {
            throw new IllegalArgumentException("A secret needs a fact and a reveal condition");
        }
        requiresEvidence = requiresEvidence == null ? List.of() : List.copyOf(requiresEvidence);
        if (Set.copyOf(requiresEvidence).size() != requiresEvidence.size()) {
            throw new IllegalArgumentException("A secret lists the same required evidence twice");
        }
        requiredCount = requiredCount == null ? requiresEvidence.size() : requiredCount;
        boolean validCount = requiresEvidence.isEmpty()
                ? requiredCount == 0
                : requiredCount >= 1 && requiredCount <= requiresEvidence.size();
        if (!validCount) {
            throw new IllegalArgumentException(
                    "The required count of a secret must be between 1 and the number of required evidence items");
        }
    }

    /** True when the team has shown the character enough of the required evidence, or when none is required. */
    public boolean isEvidenceShown(Set<RequiredEvidence> shownEvidence) {
        long shownCount =
                requiresEvidence.stream().filter(shownEvidence::contains).count();
        return shownCount >= requiredCount;
    }
}
