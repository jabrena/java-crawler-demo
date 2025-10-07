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
    PRODUCER_CONSUMER;

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
