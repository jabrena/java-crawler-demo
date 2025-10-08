package info.jab.crawler.commons;

/**
 * Default implementation of CrawlerBuilder that can create any type of crawler.
 *
 * This builder is decoupled from specific implementations and uses the CrawlerType
 * enum to determine which implementation to create.
 */
public class DefaultCrawlerBuilder implements CrawlerBuilder {

    private int maxDepth = 2;
    private int maxPages = 50;
    private int timeoutMs = 5000;
    private boolean followExternalLinks = false;
    private String startDomain = "";
    private int numThreads = 4;
    private CrawlerType crawlerType = CrawlerType.SEQUENTIAL;

    /**
     * Sets the maximum crawl depth.
     *
     * @param maxDepth maximum depth (must be non-negative)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if maxDepth is negative
     */
    @Override
    public CrawlerBuilder maxDepth(int maxDepth) {
        validateNonNegative(maxDepth, "maxDepth");
        this.maxDepth = maxDepth;
        return this;
    }

    /**
     * Sets the maximum number of pages to crawl.
     *
     * @param maxPages maximum pages (must be positive)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if maxPages is not positive
     */
    @Override
    public CrawlerBuilder maxPages(int maxPages) {
        validatePositive(maxPages, "maxPages");
        this.maxPages = maxPages;
        return this;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param timeoutMs timeout in milliseconds (must be positive)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if timeout is not positive
     */
    @Override
    public CrawlerBuilder timeout(int timeoutMs) {
        validatePositive(timeoutMs, "timeout");
        this.timeoutMs = timeoutMs;
        return this;
    }

    /**
     * Sets whether to follow external links.
     *
     * @param follow true to follow external links
     * @return this builder for method chaining
     */
    @Override
    public CrawlerBuilder followExternalLinks(boolean follow) {
        this.followExternalLinks = follow;
        return this;
    }

    /**
     * Sets the starting domain for link filtering.
     *
     * @param domain the domain to restrict crawling to
     * @return this builder for method chaining
     */
    @Override
    public CrawlerBuilder startDomain(String domain) {
        this.startDomain = domain;
        return this;
    }

    /**
     * Sets the crawler type to use.
     *
     * @param crawlerType the type of crawler to create
     * @return this builder for method chaining
     */
    public DefaultCrawlerBuilder crawlerType(CrawlerType crawlerType) {
        this.crawlerType = crawlerType;
        return this;
    }

    /**
     * Sets the number of worker threads (only applicable for multi-threaded crawlers).
     *
     * @param numThreads number of threads (must be positive)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if numThreads is not positive
     */
    public DefaultCrawlerBuilder numThreads(int numThreads) {
        validatePositive(numThreads, "numThreads");
        this.numThreads = numThreads;
        return this;
    }

    /**
     * Builds a new crawler with the configured settings.
     *
     * @return a new crawler instance
     */
    @Override
    public Crawler build() {
        return switch (crawlerType) {
            case SEQUENTIAL -> new info.jab.crawler.v1.SequentialCrawler(
                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain
            );
            case PRODUCER_CONSUMER -> new info.jab.crawler.v2.ProducerConsumerCrawler(
                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, numThreads
            );
            case RECURSIVE -> new info.jab.crawler.v3.RecursiveCrawler(
                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain
            );
            case MULTI_THREADED_ITERATIVE -> new info.jab.crawler.v4.MultiThreadedIterativeCrawler(
                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, numThreads
            );
            case ACTOR -> new info.jab.crawler.v5.ActorCrawler(
                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, numThreads
            );
            case RECURSIVE_ACTOR -> new info.jab.crawler.v6.RecursiveActorCrawler(
                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, numThreads
            );
            case STRUCTURAL_CONCURRENCY -> new info.jab.crawler.v7.StructuralConcurrencyCrawler(
                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain
            );
            case HYBRID_ACTOR_STRUCTURAL -> new info.jab.crawler.v8.HybridActorStructuralCrawler(
                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, numThreads
            );
            case STRUCTURED_WORKER -> new info.jab.crawler.v9.StructuredWorkerCrawler(
                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, numThreads
            );
        };
    }
}
