package nl.rubium.tegenspraak.casefile.entity;

/** A mail or file on a desktop of the case that the team must show a character before it gives up a secret. */
public record RequiredEvidence(String desktop, String app, String item) {

    public RequiredEvidence {
        if (desktop == null || desktop.isBlank() || app == null || app.isBlank() || item == null || item.isBlank()) {
            throw new IllegalArgumentException("Required evidence needs a desktop, an app and an item");
        }
    }
}
