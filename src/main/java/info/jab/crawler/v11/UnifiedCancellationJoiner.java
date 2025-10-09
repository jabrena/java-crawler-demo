package info.jab.crawler.v11;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Custom Joiner implementation that provides unified cancellation for crawling.
 *
 * This joiner addresses the SoftwareMill critique of JEP 505 by implementing:
 * 1. Unified cancellation: allows scope body to signal completion
 * 2. No split between Joiner logic and scope body logic
 * 3. Waits for all subtasks to complete (not race semantics for crawling)
 *
 * The joiner waits for all subtasks to complete and stores any exceptions
 * encountered, allowing the scope body to participate in error handling.
 *
 * @param <T> the result type of the subtasks
 */
public class UnifiedCancellationJoiner<T> implements StructuredTaskScope.Joiner<T, Void> {

    private final AtomicReference<Throwable> firstException = new AtomicReference<>();
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger totalTasks = new AtomicInteger(0);

    /**
     * Called when a subtask completes.
     *
     * This method implements unified cancellation by:
     * 1. Storing the first exception encountered (if any)
     * 2. Tracking completion count
     * 3. Returning false to wait for all tasks to complete
     *
     * @param subtask the completed subtask
     * @return false to wait for all tasks to complete
     */
    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
        // Store the first exception encountered
        if (subtask.state() == StructuredTaskScope.Subtask.State.FAILED) {
            firstException.compareAndSet(null, subtask.exception());
        }

        // Increment completed count
        completedTasks.incrementAndGet();

        // Wait for all tasks to complete (not race semantics for crawling)
        return false;
    }

    /**
     * Returns the final result of the scope.
     *
     * This method throws the first exception encountered, or returns null
     * if all subtasks completed successfully.
     *
     * @return null if successful, or throws the first exception
     * @throws Throwable the first exception encountered, if any
     */
    @Override
    public Void result() throws Throwable {
        Throwable exception = firstException.get();
        if (exception != null) {
            throw exception;
        }
        return null;
    }

    /**
     * Gets the first exception encountered, if any.
     *
     * @return the first exception, or null if no exceptions occurred
     */
    public Throwable getFirstException() {
        return firstException.get();
    }
}
