package info.jab.crawler.commons;

/**
 * Enumeration of available crawler implementations.
 *
 * This enum provides a way to select different crawler implementations
 * and serves as a factory for creating crawler builders.
 */
public enum CrawlerType {

    /**
     * Sequential crawler implementation.
     * Processes URLs one by one in a single thread.
     */
    SEQUENTIAL,

    /**
     * Producer-consumer crawler implementation.
     * Uses multiple threads with a producer-consumer pattern for parallel processing.
     */
    PRODUCER_CONSUMER,

    /**
     * Recursive crawler implementation.
     * Uses recursive approach with trampoline pattern for safe deep recursion.
     */
    RECURSIVE,

    /**
     * Multi-threaded iterative crawler implementation.
     * Uses producer-consumer pattern with multiple worker threads for parallel processing.
     * Provides high performance through concurrent execution without recursion or trampoline.
     */
    MULTI_THREADED_ITERATIVE,

    /**
     * Actor model crawler implementation.
     * Uses CompletableFuture-based actors with supervisor pattern for coordination.
     * Provides fault tolerance, message passing, and parallel processing.
     */
    ACTOR,

    /**
     * Recursive actor model crawler implementation.
     * Combines actor model with recursive design for natural tree-like crawling.
     * Each actor can spawn child actors for discovered links, creating dynamic actor trees.
     * Uses trampoline pattern for safe deep recursion and message passing for coordination.
     */
    RECURSIVE_ACTOR,

    /**
     * Structural concurrency crawler implementation.
     * Uses Java 25's StructuredTaskScope for managing concurrent subtasks within well-defined scopes.
     * Provides automatic cancellation, cleanup, and simplified error handling.
     * Natural tree-like crawling structure with proper resource management through structured scoping.
     */
    STRUCTURAL_CONCURRENCY,

    /**
     * Hybrid actor-structural concurrency crawler implementation.
     * Combines actor model pattern for coordination and state management with structural concurrency for actual crawling work.
     * Uses supervisor actor for fault tolerance and message-based coordination while leveraging StructuredTaskScope for efficient parallel crawling.
     * Provides the best of both paradigms: actor-based coordination with automatic resource management.
     */
    HYBRID_ACTOR_STRUCTURAL,

    /**
     * Structured worker crawler implementation.
     * Combines BlockingQueue + worker pattern with StructuredTaskScope.
     * Uses structured concurrency for worker management with queue-based coordination.
     * Provides automatic resource cleanup, better error handling, and virtual threads
     * while maintaining the proven producer-consumer pattern from v4.
     */
    STRUCTURED_WORKER;

    /**
     * Creates a new builder instance for the selected crawler type.
     *
     * @return a new builder instance configured for this crawler type
     */
    public CrawlerBuilder createBuilder() {
        return new DefaultCrawlerBuilder().crawlerType(this);
    }

    /**
     * Creates a new crawler instance with default configuration.
     *
     * @return a new crawler instance with default settings
     */
    public Crawler createDefault() {
        return createBuilder().build();
    }

    /**
     * Creates a new crawler instance with custom configuration.
     *
     * @param configurator function to configure the builder
     * @return a new crawler instance with custom settings
     */
    public Crawler createWith(CrawlerConfigurator configurator) {
        CrawlerBuilder builder = createBuilder();
        configurator.configure(builder);
        return builder.build();
    }

    /**
     * Functional interface for configuring crawler builders.
     */
    @FunctionalInterface
    public interface CrawlerConfigurator {
        void configure(CrawlerBuilder builder);
    }
}
