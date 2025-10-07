package info.jab.crawler.v3;

import java.util.function.Supplier;

/**
 * Trampoline implementation for safe recursion without stack overflow.
 *
 * This class provides a way to implement recursive algorithms that would
 * normally cause stack overflow by converting recursive calls into iterative
 * loops using continuation functions.
 *
 * The trampoline pattern allows for deep recursion by:
 * 1. Returning a continuation function instead of making recursive calls
 * 2. Iteratively executing continuations in a loop
 * 3. Avoiding stack frame accumulation
 */
public class Trampoline<T> {

    /**
     * Represents the result of a trampoline computation.
     * Either a final value or a continuation to be executed.
     */
    public static class Result<T> {
        private final T value;
        private final Supplier<Trampoline<T>> continuation;
        private final boolean isComplete;

        private Result(T value, Supplier<Trampoline<T>> continuation, boolean isComplete) {
            this.value = value;
            this.continuation = continuation;
            this.isComplete = isComplete;
        }

        /**
         * Creates a completed result with a final value.
         */
        public static <T> Result<T> done(T value) {
            return new Result<>(value, null, true);
        }

        /**
         * Creates a continuation result that needs further processing.
         */
        public static <T> Result<T> more(Supplier<Trampoline<T>> continuation) {
            return new Result<>(null, continuation, false);
        }

        public boolean isComplete() {
            return isComplete;
        }

        public T getValue() {
            return value;
        }

        public Supplier<Trampoline<T>> getContinuation() {
            return continuation;
        }
    }

    private final Result<T> result;

    private Trampoline(Result<T> result) {
        this.result = result;
    }

    /**
     * Creates a completed trampoline with a final value.
     */
    public static <T> Trampoline<T> done(T value) {
        return new Trampoline<>(Result.done(value));
    }

    /**
     * Creates a trampoline that needs further processing.
     */
    public static <T> Trampoline<T> more(Supplier<Trampoline<T>> continuation) {
        return new Trampoline<>(Result.more(continuation));
    }

    /**
     * Executes the trampoline until completion, returning the final result.
     * This method safely handles deep recursion without stack overflow.
     */
    public T run() {
        Trampoline<T> current = this;

        while (!current.result.isComplete()) {
            current = current.result.getContinuation().get();
        }

        return current.result.getValue();
    }

    /**
     * Executes the trampoline with a maximum number of steps to prevent infinite loops.
     *
     * @param maxSteps maximum number of steps to execute
     * @return the final result or null if max steps exceeded
     */
    public T runWithLimit(int maxSteps) {
        Trampoline<T> current = this;
        int steps = 0;

        while (!current.result.isComplete() && steps < maxSteps) {
            current = current.result.getContinuation().get();
            steps++;
        }

        if (steps >= maxSteps) {
            throw new RuntimeException("Trampoline execution exceeded maximum steps: " + maxSteps);
        }

        return current.result.getValue();
    }
}
