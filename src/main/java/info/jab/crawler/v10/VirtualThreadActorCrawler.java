package info.jab.crawler.v10;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Virtual thread-based actor web crawler using lightweight concurrency and message passing.
 *
 * This crawler implements the Actor model pattern with:
 * - Virtual thread-based actors for lightweight concurrency
 * - Supervisor actor for coordination and state management
 * - Worker actors for processing individual URLs
 * - Asynchronous message passing between actors
 * - Thread-safe shared state using concurrent collections
 * - Fault tolerance through supervisor pattern
 *
 * Design characteristics:
 * - Virtual thread-based actors for efficient concurrency without thread pool overhead
 * - Supervisor pattern for fault tolerance and coordination
 * - Message passing for actor communication
 * - Breadth-first traversal with parallel execution
 * - Maintains visited URLs to avoid duplicates
 * - Respects maximum depth and page limits
 * - Simplified async processing compared to CompletableFuture-based approaches
 *
 * Key advantages over V5 (CompletableFuture-based):
 * - Simpler code without complex async chaining
 * - Better resource utilization with virtual threads
 * - Easier debugging and stack traces
 * - No thread pool management complexity
 * - Natural blocking code patterns
 */
public class VirtualThreadActorCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int numActors;

    /**
     * Creates a new virtual thread-based actor crawler.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     * @param numActors number of worker actors to create
     */
    public VirtualThreadActorCrawler(int maxDepth, int maxPages, int timeoutMs,
                                   boolean followExternalLinks, String startDomain, int numActors) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
        this.numActors = numActors;
    }

    /**
     * Crawls the web starting from the given seed URL using virtual thread actors.
     *
     * @param seedUrl the starting URL for the crawl
     * @return CrawlResult containing all crawled pages and statistics
     */
    @Override
    public CrawlResult crawl(String seedUrl) {
        if (seedUrl == null || seedUrl.isBlank()) {
            throw new IllegalArgumentException("Seed URL cannot be null or empty");
        }

        // Create supervisor actor
        SupervisorActor supervisor = new SupervisorActor(
            maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, numActors
        );

        try {
            // Start crawling and wait for completion
            CompletableFuture<CrawlResult> crawlFuture = supervisor.start(seedUrl);

            // Wait for completion with timeout
            return crawlFuture.get(timeoutMs, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            // Handle any errors during crawling
            throw new RuntimeException("Virtual thread actor crawling failed: " + e.getMessage(), e);
        } finally {
            // Ensure supervisor is shut down
            supervisor.shutdown();
        }
    }
}
