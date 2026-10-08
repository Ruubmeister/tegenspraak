package nl.rubium.tegenspraak.investigation.control;

/** The stored key of something a team can unlock. Ids are kebab-case, so the separators cannot clash. */
public record UnlockTarget(String key) {

    public static UnlockTarget character(String characterId) {
        return new UnlockTarget("character:" + characterId);
    }

    public static UnlockTarget desktop(String desktopId) {
        return new UnlockTarget("desktop:" + desktopId);
    }

    public static UnlockTarget item(String desktopId, String appId, String itemId) {
        return new UnlockTarget("item:" + desktopId + "/" + appId + "/" + itemId);
    }
}
