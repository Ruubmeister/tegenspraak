package nl.rubium.tegenspraak.casefile.entity;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;
import nl.rubium.tegenspraak.desktop.entity.ItemRef;

/**
 * Something a character hides until {@code revealWhen} is met. When {@code requiresEvidence} is set, the team must also
 * have shown the character at least {@code requiredCount} of those desktop items as attachments; until then the
 * character denies it, whatever the team claims. Without a count, all of them are required. With a {@code minimumMood},
 * the character also keeps the secret until it feels at least that good about the team.
 */
public record CharacterSecret(
        @NotBlank String fact,
        @NotBlank String revealWhen,
        List<ItemRef> requiresEvidence,
        Integer requiredCount,
        Mood minimumMood) {

    public CharacterSecret {
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
    public boolean isEvidenceShown(Set<ItemRef> shownEvidence) {
        long shownCount =
                requiresEvidence.stream().filter(shownEvidence::contains).count();
        return shownCount >= requiredCount;
    }

    public boolean allows(Mood mood) {
        return minimumMood == null || mood.isAtLeast(minimumMood);
    }
}
