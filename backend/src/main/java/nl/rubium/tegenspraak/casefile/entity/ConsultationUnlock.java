package nl.rubium.tegenspraak.casefile.entity;

/**
 * What opens for the team once a contact answered a consultation: a hidden character ({@code character}), a locked
 * desktop ({@code desktop}) or a locked item on a desktop ({@code desktop}, {@code app} and {@code item}).
 */
public record ConsultationUnlock(String character, String desktop, String app, String item) {

    public ConsultationUnlock {
        boolean validCharacter = character != null && desktop == null && app == null && item == null;
        boolean validDesktop = character == null && desktop != null && app == null && item == null;
        boolean validItem = character == null && desktop != null && app != null && item != null;
        if (!validCharacter && !validDesktop && !validItem) {
            throw new IllegalArgumentException(
                    "A consultation unlocks either a character, a desktop, or a desktop with an app and an item");
        }
    }

    public boolean isCharacter() {
        return character != null;
    }

    public boolean isItem() {
        return item != null;
    }
}
