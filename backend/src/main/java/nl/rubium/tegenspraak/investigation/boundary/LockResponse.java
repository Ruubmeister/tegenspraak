package nl.rubium.tegenspraak.investigation.boundary;

import nl.rubium.tegenspraak.desktop.entity.Lock;

/** Shown while something is still locked: only the hint, never the code. Null once open or when never locked. */
public record LockResponse(String hint) {

    static LockResponse whileLocked(Lock lock, boolean open) {
        return open ? null : new LockResponse(lock.hint());
    }
}
