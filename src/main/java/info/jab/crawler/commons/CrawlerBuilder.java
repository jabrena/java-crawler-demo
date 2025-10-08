package info.jab.crawler.commons;

/**
 * Common builder interface for web crawlers.
 *
 * This interface defines the standard configuration options that all crawler
 * implementations should support, following the builder pattern for fluent
 * configuration.
 */
public interface CrawlerBuilder {

    /**
     * Sets the maximum crawl depth.
     *
     * @param maxDepth maximum depth (must be non-negative)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if maxDepth is negative
     */
    CrawlerBuilder maxDepth(int maxDepth);

    /**
     * Sets the maximum number of pages to crawl.
     *
     * @param maxPages maximum pages (must be positive)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if maxPages is not positive
     */
    CrawlerBuilder maxPages(int maxPages);

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param timeoutMs timeout in milliseconds (must be positive)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if timeout is not positive
     */
    CrawlerBuilder timeout(int timeoutMs);

    /**
     * Sets whether to follow external links.
     *
     * @param follow true to follow external links
     * @return this builder for method chaining
     */
    CrawlerBuilder followExternalLinks(boolean follow);

    /**
     * Sets the starting domain for link filtering.
     *
     * @param domain the domain to restrict crawling to
     * @return this builder for method chaining
     */
    CrawlerBuilder startDomain(String domain);

    /**
     * Sets the number of worker threads (only applicable for multi-threaded crawlers).
     * Default implementation does nothing for single-threaded crawlers.
     *
     * @param numThreads number of threads (must be positive)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if numThreads is not positive
     */
    default CrawlerBuilder numThreads(int numThreads) {
        // Default implementation does nothing for single-threaded crawlers
        return this;
    }

    /**
     * Builds a new crawler with the configured settings.
     *
     * @return a new crawler instance
     */
    Crawler build();

    /**
     * Pure validation function for positive integers.
     */
    default void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /**
     * Pure validation function for non-negative integers.
     */
    default void validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }
}
