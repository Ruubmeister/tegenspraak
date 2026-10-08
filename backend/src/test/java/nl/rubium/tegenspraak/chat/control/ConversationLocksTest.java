package nl.rubium.tegenspraak.chat.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConversationLocksTest {

    private static final Duration GRACE = Duration.ofMillis(200);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final ConversationLocks conversationLocks = new ConversationLocks();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final CountDownLatch firstEntered = new CountDownLatch(1);
    private final CountDownLatch releaseFirst = new CountDownLatch(1);

    @AfterEach
    void tearDown() {
        releaseFirst.countDown();
        executor.shutdownNow();
    }

    @Test
    void withLock_givenSameConversationBusy_expectSecondWaitsUntilFirstFinishes() throws Exception {
        Conversation conversation = conversation();
        holdLock(conversation);

        CompletableFuture<String> second = runWithLock(conversation, "tweede");

        assertThat(second).failsWithin(GRACE);
        releaseFirst.countDown();
        assertThat(second.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isEqualTo("tweede");
    }

    @Test
    void withLock_givenOtherConversationBusy_expectNoWaiting() throws Exception {
        holdLock(conversation());

        CompletableFuture<String> other = runWithLock(conversation(), "ander");

        assertThat(other.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isEqualTo("ander");
    }

    /** Takes the lock on another thread and keeps it until releaseFirst opens. */
    private void holdLock(Conversation conversation) throws InterruptedException {
        CompletableFuture.supplyAsync(
                () -> conversationLocks.withLock(conversation, () -> {
                    firstEntered.countDown();
                    awaitUninterruptibly(releaseFirst);
                    return "eerste";
                }),
                executor);
        assertThat(firstEntered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .isTrue();
    }

    private CompletableFuture<String> runWithLock(Conversation conversation, String result) {
        return CompletableFuture.supplyAsync(() -> conversationLocks.withLock(conversation, () -> result), executor);
    }

    private static Conversation conversation() {
        return new Conversation(UUID.randomUUID(), "demo", "de-butler");
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
