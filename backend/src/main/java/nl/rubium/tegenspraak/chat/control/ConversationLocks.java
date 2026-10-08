package nl.rubium.tegenspraak.chat.control;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Handles one message per conversation at a time. Without it, two team members writing to the same character at once
 * would both get a reply built on a history that misses the other's message. The application runs as a single
 * instance, so an in-memory lock is enough. There is one small lock per conversation that was ever used since startup.
 */
@Component
public class ConversationLocks {

    private final Map<Conversation, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(Conversation conversation, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(conversation, key -> new ReentrantLock(true));
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
