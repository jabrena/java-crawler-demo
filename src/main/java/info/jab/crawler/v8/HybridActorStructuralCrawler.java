package info.jab.crawler.v8;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;

/**
 * Hybrid Actor-Structural Concurrency web crawler combining the best of both paradigms.
 *
 * This crawler implements a hybrid approach that combines:
 * - Actor model pattern for coordination, state management, and fault tolerance
 * - Structural concurrency for actual crawling work with automatic resource management
 * - Message passing for actor coordination and state updates
 * - StructuredTaskScope for efficient parallel crawling with virtual threads
 * - Automatic cleanup and cancellation propagation
 *
 * Design characteristics:
 * - Supervisor actor manages state, coordination, and message processing
 * - Structural concurrency handles actual page fetching and link following
 * - Natural tree-like crawling structure with automatic resource management
 * - Fault isolation - actor coordination failures don't affect crawling tasks
 * - Virtual threads for efficient I/O operations
 * - Automatic cancellation and cleanup when scope closes
 * - Breadth-first traversal with parallel execution
 * - Maintains visited URLs to avoid duplicates
 * - Respects maximum depth and page limits
 */
public class HybridActorStructuralCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int maxConcurrentTasks;

    /**
     * Creates a new hybrid actor-structural concurrency crawler.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     * @param maxConcurrentTasks maximum number of concurrent crawling tasks
     */
    public HybridActorStructuralCrawler(int maxDepth, int maxPages, int timeoutMs,
                                       boolean followExternalLinks, String startDomain, int maxConcurrentTasks) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    /**
     * Crawls the web starting from the given seed URL using hybrid actor-structural concurrency.
     *
     * @param seedUrl the starting URL for the crawl
     * @return CrawlResult containing all crawled pages and statistics
     */
    @Override
    public CrawlResult crawl(String seedUrl) {
        if (seedUrl == null || seedUrl.isBlank()) {
            throw new IllegalArgumentException("Seed URL cannot be null or empty");
        }

        long startTime = System.currentTimeMillis();

        // Create supervisor actor for coordination and state management
        SupervisorActor supervisor = new SupervisorActor(
            maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, maxConcurrentTasks
        );

        try {
            // Use structural concurrency for the entire crawling process
            return supervisor.crawlWithStructuralConcurrency(seedUrl, startTime);

        } catch (Exception e) {
            // Handle any errors during crawling
            throw new RuntimeException("Hybrid actor-structural crawling failed: " + e.getMessage(), e);
        } finally {
            // Ensure supervisor is shut down
            supervisor.shutdown();
        }
    }
}
