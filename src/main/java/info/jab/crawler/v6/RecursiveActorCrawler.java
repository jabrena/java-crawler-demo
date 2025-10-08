package info.jab.crawler.v6;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Recursive Actor-based web crawler combining actor model with recursive design.
 *
 * This crawler implements a hybrid approach that combines:
 * - Actor model pattern for asynchronous processing and fault tolerance
 * - Recursive design for natural tree-like crawling structure
 * - Asynchronous execution with CompletableFuture for safe deep recursion
 * - Dynamic actor spawning based on discovered links
 * - Message passing for coordination between actors
 *
 * Design characteristics:
 * - Each actor can recursively spawn child actors for discovered links
 * - Natural tree-like crawling structure matching web topology
 * - Asynchronous processing with CompletableFuture
 * - Fault isolation - actor failures don't affect other branches
 * - Dynamic resource management - actors created on-demand
 * - Breadth-first traversal with parallel execution
 * - Maintains visited URLs to avoid duplicates
 * - Respects maximum depth and page limits
 */
public class RecursiveActorCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int maxActors;

    /**
     * Creates a new recursive actor-based crawler.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     * @param maxActors maximum number of concurrent actors
     */
    public RecursiveActorCrawler(int maxDepth, int maxPages, int timeoutMs,
                                boolean followExternalLinks, String startDomain, int maxActors) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
        this.maxActors = maxActors;
    }

    /**
     * Crawls the web starting from the given seed URL using recursive actor model.
     *
     * @param seedUrl the starting URL for the crawl
     * @return CrawlResult containing all crawled pages and statistics
     */
    @Override
    public CrawlResult crawl(String seedUrl) {
        if (seedUrl == null || seedUrl.isBlank()) {
            throw new IllegalArgumentException("Seed URL cannot be null or empty");
        }

        // Create root actor for the seed URL
        RecursiveActor rootActor = new RecursiveActor(
            maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, maxActors
        );

        try {
            // Start recursive crawling and wait for completion
            CompletableFuture<CrawlResult> crawlFuture = rootActor.crawlRecursively(seedUrl, 0);

            // Wait for completion with timeout
            return crawlFuture.get(timeoutMs * 2, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            // Handle any errors during crawling
            throw new RuntimeException("Recursive actor crawling failed: " + e.getMessage(), e);
        } finally {
            // Ensure root actor is shut down
            rootActor.shutdown();
        }
    }
}
