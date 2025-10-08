package info.jab.crawler.commons;

import java.util.function.Supplier;

/**
 * A unified trampoline implementation for managing recursive computations without stack overflow.
 * This allows for deep recursion by converting recursive calls into an iterative process.
 *
 * This implementation combines the best features from both V3 and V4:
 * - Modern sealed interface design (from V4)
 * - Thread-safe execution (from V4)
 * - Step limiting capability (from V3)
 * - Simple and clean API
 *
 * @param <T> the type of the final result
 */
public sealed interface Trampoline<T> {

    /**
     * Returns the final result of the computation.
     *
     * @return the result
     * @throws IllegalStateException if called on a 'more' state
     */
    T get();

    /**
     * Indicates if the computation is complete.
     *
     * @return true if done, false otherwise
     */
    boolean isDone();

    /**
     * Returns the next step in the computation if not done.
     *
     * @return a supplier for the next trampoline step
     * @throws IllegalStateException if called on a 'done' state
     */
    Trampoline<T> resume();

    /**
     * Executes the trampoline computation iteratively until a final result is reached.
     * This method is thread-safe and can be called from multiple threads.
     *
     * @return the final result
     */
    default T run() {
        Trampoline<T> current = this;
        while (!current.isDone()) {
            current = current.resume();
        }
        return current.get();
    }

    /**
     * Executes the trampoline with a maximum number of steps to prevent infinite loops.
     * This is useful for debugging or when you want to limit execution time.
     *
     * @param maxSteps maximum number of steps to execute
     * @return the final result
     * @throws RuntimeException if max steps exceeded
     */
    default T runWithLimit(int maxSteps) {
        Trampoline<T> current = this;
        int steps = 0;

        while (!current.isDone() && steps < maxSteps) {
            current = current.resume();
            steps++;
        }

        if (steps >= maxSteps) {
            throw new RuntimeException("Trampoline execution exceeded maximum steps: " + maxSteps);
        }

        return current.get();
    }

    /**
     * Represents a completed state of the trampoline.
     * This record is immutable and thread-safe.
     *
     * @param <T> the type of the result
     */
    record Done<T>(T result) implements Trampoline<T> {
        @Override
        public T get() {
            return result;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Trampoline<T> resume() {
            throw new IllegalStateException("Cannot resume a done trampoline");
        }
    }

    /**
     * Represents a continuation state of the trampoline.
     * This record is immutable and thread-safe.
     *
     * @param <T> the type of the result
     */
    record More<T>(Supplier<Trampoline<T>> next) implements Trampoline<T> {
        @Override
        public T get() {
            throw new IllegalStateException("Cannot get result from a more trampoline");
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Trampoline<T> resume() {
            return next.get();
        }
    }

    /**
     * Factory method for a 'done' trampoline state.
     *
     * @param result the final result
     * @param <T> the type of the result
     * @return a new Done instance
     */
    static <T> Trampoline<T> done(T result) {
        return new Done<>(result);
    }

    /**
     * Factory method for a 'more' trampoline state.
     *
     * @param next a supplier for the next trampoline step
     * @param <T> the type of the result
     * @return a new More instance
     */
    static <T> Trampoline<T> more(Supplier<Trampoline<T>> next) {
        return new More<>(next);
    }
}
