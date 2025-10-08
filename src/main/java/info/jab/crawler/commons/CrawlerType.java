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
     * Multi-threaded recursive crawler implementation.
     * Combines multi-threading with recursive design using trampoline pattern.
     * Provides parallel performance with stack-safe deep recursion.
     */
    MULTI_THREADED_RECURSIVE,

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
    RECURSIVE_ACTOR;

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
