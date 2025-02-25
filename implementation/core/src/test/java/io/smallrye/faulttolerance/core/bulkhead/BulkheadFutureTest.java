package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.sync;
import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class BulkheadFutureTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(4));
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldLetOneIn() throws Throwable {
        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> {
            return completedFuture("shouldLetSingleThrough");
        });
        Bulkhead<java.util.concurrent.Future<String>> bulkhead = new Bulkhead<>(invocation,
                "shouldLetSingleThrough", 2, 2, true);
        assertThat(bulkhead.apply(sync(null)).awaitBlocking().get())
                .isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxIn() throws Exception { // max threads + max queue
        Barrier delayBarrier = Barrier.noninterruptible();
        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return completedFuture("shouldLetMaxThrough");
        });
        Bulkhead<java.util.concurrent.Future<String>> bulkhead = new Bulkhead<>(invocation,
                "shouldLetSingleThrough", 2, 3, true);

        List<TestThread<java.util.concurrent.Future<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead, false));
        }
        delayBarrier.open();
        for (int i = 0; i < 5; i++) {
            assertThat(threads.get(i).await().get()).isEqualTo("shouldLetMaxThrough");
        }
    }

    @Test
    public void shouldRejectMaxPlus1() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return completedFuture("shouldRejectMaxPlus1");
        });
        Bulkhead<java.util.concurrent.Future<String>> bulkhead = new Bulkhead<>(invocation,
                "shouldRejectMaxPlus1", 2, 3, true);

        List<TestThread<java.util.concurrent.Future<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead, false));
        }
        waitUntilQueueSize(bulkhead, 3, Duration.ofMillis(500));

        assertThatThrownBy(bulkhead.apply(sync(null))::awaitBlocking)
                .isInstanceOf(BulkheadException.class);

        delayBarrier.open();
        for (int i = 0; i < 5; i++) {
            assertThat(threads.get(i).await().get()).isEqualTo("shouldRejectMaxPlus1");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Left() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            return completedFuture("shouldLetMaxPlus1After1Left");
        });

        Bulkhead<java.util.concurrent.Future<String>> bulkhead = new Bulkhead<>(invocation,
                "shouldLetMaxPlus1After1Left", 2, 3, true);

        List<TestThread<java.util.concurrent.Future<String>>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead, false));
        }

        delayBarrier.open();

        TestThread<java.util.concurrent.Future<String>> finishedThread = getSingleFinishedThread(threads,
                Duration.ofSeconds(1));
        threads.remove(finishedThread);
        assertThat(finishedThread.await().get()).isEqualTo("shouldLetMaxPlus1After1Left");

        threads.add(runOnTestThread(bulkhead, false));
        letOneInSemaphore.release(100);
        for (TestThread<java.util.concurrent.Future<String>> thread : threads) {
            assertThat(thread.await().get()).isEqualTo("shouldLetMaxPlus1After1Left");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Failed() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        Semaphore letOneInSemaphore = new Semaphore(0);
        Semaphore finishedThreadsCount = new Semaphore(0);

        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> {
            letOneInSemaphore.acquire();
            finishedThreadsCount.release();
            throw error;
        });

        Bulkhead<java.util.concurrent.Future<String>> bulkhead = new Bulkhead<>(invocation,
                "shouldLetMaxPlus1After1Left", 2, 3, true);

        List<TestThread<java.util.concurrent.Future<String>>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead, false));
        }

        letOneInSemaphore.release();
        finishedThreadsCount.acquire();

        TestThread<java.util.concurrent.Future<String>> finishedThread = getSingleFinishedThread(threads,
                Duration.ofSeconds(1));
        assertThatThrownBy(finishedThread::await).isEqualTo(error);
        threads.remove(finishedThread);

        threads.add(runOnTestThread(bulkhead, false));

        letOneInSemaphore.release(5);
        for (TestThread<java.util.concurrent.Future<String>> thread : threads) {
            finishedThreadsCount.acquire();
            assertThatThrownBy(thread::await).isEqualTo(error);
        }
    }

    // put five elements into the bulkhead,
    // check another one cannot be inserted
    // cancel one,
    // insert another one
    // run the tasks and check results
    @Test
    public void shouldLetMaxPlus1After1Canceled() throws Exception {
        Party party = Party.create(2);

        TestInvocation<java.util.concurrent.Future<String>> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return completedFuture("shouldLetMaxPlus1After1Canceled");
        });

        Bulkhead<java.util.concurrent.Future<String>> bulkhead = new Bulkhead<>(invocation,
                "shouldLetMaxPlus1After1Canceled", 2, 3, true);

        List<TestThread<java.util.concurrent.Future<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(bulkhead, false));
        }
        // to make sure the fifth added is enqueued, wait until two are started
        party.organizer().waitForAll();

        threads.add(runOnTestThread(bulkhead, false));

        waitUntilQueueSize(bulkhead, 3, Duration.ofSeconds(1));

        TestThread<java.util.concurrent.Future<String>> failedThread = runOnTestThread(bulkhead, false);
        assertThatThrownBy(failedThread::await).isInstanceOf(BulkheadException.class);

        threads.remove(4).interrupt(); // cancel and remove from the list
        waitUntilQueueSize(bulkhead, 2, Duration.ofSeconds(1));

        threads.add(runOnTestThread(bulkhead, false));
        waitUntilQueueSize(bulkhead, 3, Duration.ofSeconds(1));

        party.organizer().disband();

        for (TestThread<java.util.concurrent.Future<String>> thread : threads) {
            assertThat(thread.await().get()).isEqualTo("shouldLetMaxPlus1After1Canceled");
        }
    }

    private void waitUntilQueueSize(Bulkhead<java.util.concurrent.Future<String>> bulkhead, int size, Duration timeout) {
        int maxQueueSize = 3; // all tests that call this method have queue size == 3
        await().pollInterval(Duration.ofMillis(50))
                .atMost(timeout)
                .until(() -> {
                    int queueSize = Math.max(0, maxQueueSize - bulkhead.getAvailableCapacityPermits());
                    return queueSize == size;
                });
    }

    private <V> TestThread<java.util.concurrent.Future<V>> getSingleFinishedThread(
            List<TestThread<java.util.concurrent.Future<V>>> threads, Duration timeout) {
        return await().atMost(timeout)
                .until(() -> getSingleFinishedThread(threads), Optional::isPresent)
                .get();
    }

    private static <V> Optional<TestThread<java.util.concurrent.Future<V>>> getSingleFinishedThread(
            List<TestThread<java.util.concurrent.Future<V>>> threads) {
        for (TestThread<java.util.concurrent.Future<V>> thread : threads) {
            if (thread.isDone()) {
                return Optional.of(thread);
            }
        }
        return Optional.empty();
    }
}
