package info.jab.crawler.v11;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

/**
 * Utility class implementing timeout-as-method pattern from SoftwareMill article.
 *
 * This utility addresses the critique that timeout should be implemented as a method
 * rather than a configuration parameter. It demonstrates how to create lightweight
 * timeout functionality using StructuredTaskScope with race semantics.
 *
 * The timeout method creates a scope with two forks:
 * 1. A timeout task that sleeps for the specified duration
 * 2. The actual body task that performs the work
 *
 * The first task to complete wins, providing natural timeout behavior.
 */
public class TimeoutUtil {

    /**
     * Executes a task with a timeout, using race semantics.
     *
     * This method implements the timeout-as-method pattern by creating a
     * StructuredTaskScope with two competing tasks:
     * - A timeout task that throws TimeoutException after the duration
     * - The body task that performs the actual work
     *
     * The first task to complete wins, providing automatic timeout behavior
     * without requiring special configuration parameters.
     *
     * @param <T> the result type of the task
     * @param duration the maximum time to wait for the task to complete
     * @param body the task to execute
     * @return the result of the body task
     * @throws TimeoutException if the task doesn't complete within the duration
     * @throws InterruptedException if the current thread is interrupted
     * @throws Exception if the body task throws an exception
     */
    public static <T> T timeout(Duration duration, Callable<T> body)
            throws InterruptedException, Exception {

        try (var scope = StructuredTaskScope.open(new FirstComplete<>())) {
            // Fork the timeout task
            scope.fork(() -> {
                Thread.sleep(duration.toMillis());
                throw new TimeoutException("Task timed out after " + duration);
            });

            // Fork the body task
            var bodyTask = scope.fork(body);

            // Wait for either task to complete
            scope.join();

            // Return the body result (or throw timeout exception)
            return bodyTask.get();
        } catch (StructuredTaskScope.FailedException e) {
            // Unwrap the actual exception from StructuredTaskScope
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            } else if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw e;
            }
        }
    }

    /**
     * Custom Joiner that implements race semantics for timeout behavior.
     *
     * This joiner completes when the first task finishes, whether it's the
     * timeout task (which throws TimeoutException) or the body task (which
     * returns a result or throws an exception).
     *
     * @param <T> the result type
     */
    private static class FirstComplete<T> implements StructuredTaskScope.Joiner<T, Void> {

        private volatile Throwable firstException;

        @Override
        public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
            if (subtask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                firstException = subtask.exception();
            }
            // Return true to complete on first task completion (race semantics)
            return true;
        }

        @Override
        public Void result() throws Throwable {
            if (firstException != null) {
                throw firstException;
            }
            return null;
        }
    }
}
