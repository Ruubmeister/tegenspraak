package nl.rubium.tegenspraak.desktop.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Locks are addressed by desktop, app and item id, so those ids must be present and unique within their parent. */
final class UniqueIds {

    private UniqueIds() {}

    static void require(List<String> ids, String kind, String parentId) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Every " + kind + " in '" + parentId + "' needs an id");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate " + kind + " id '" + id + "' in '" + parentId + "'");
            }
        }
    }
}
