package info.jab.crawler.v6;

import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Recursive actor that can spawn child actors for discovered links.
 *
 * This actor is responsible for:
 * - Processing a single URL and extracting links
 * - Recursively spawning child actors for discovered links
 * - Coordinating with child actors via message passing
 * - Aggregating results from child actors
 * - Managing actor lifecycle and resource limits
 * - Asynchronous execution with CompletableFuture for safe deep recursion
 *
 * The actor processes URLs asynchronously and can spawn child actors
 * dynamically based on discovered links, creating a natural tree structure.
 */
public class RecursiveActor {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int maxActors;

    // Thread-safe collections for coordination
    private final ConcurrentHashMap<String, Boolean> visitedUrls;
    private final List<Page> successfulPages;
    private final List<String> failedUrls;
    private final AtomicInteger pagesCrawled;
    private final AtomicInteger activeActors;

    // Actor management
    private final ExecutorService executor;
    private final List<RecursiveActor> childActors;
    private volatile boolean isShuttingDown;

    /**
     * Creates a new recursive actor.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     * @param maxActors maximum number of concurrent actors
     */
    public RecursiveActor(int maxDepth, int maxPages, int timeoutMs,
                         boolean followExternalLinks, String startDomain, int maxActors) {
        this(maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, maxActors,
             new ConcurrentHashMap<>(), Collections.synchronizedList(new ArrayList<>()),
             Collections.synchronizedList(new ArrayList<>()), new AtomicInteger(0), new AtomicInteger(0));
    }

    /**
     * Creates a new recursive actor with shared state.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     * @param maxActors maximum number of concurrent actors
     * @param visitedUrls shared visited URLs map
     * @param successfulPages shared successful pages list
     * @param failedUrls shared failed URLs list
     * @param pagesCrawled shared pages crawled counter
     * @param activeActors shared active actors counter
     */
    private RecursiveActor(int maxDepth, int maxPages, int timeoutMs,
                          boolean followExternalLinks, String startDomain, int maxActors,
                          ConcurrentHashMap<String, Boolean> visitedUrls,
                          List<Page> successfulPages,
                          List<String> failedUrls,
                          AtomicInteger pagesCrawled,
                          AtomicInteger activeActors) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
        this.maxActors = maxActors;

        // Use shared thread-safe collections
        this.visitedUrls = visitedUrls;
        this.successfulPages = successfulPages;
        this.failedUrls = failedUrls;
        this.pagesCrawled = pagesCrawled;
        this.activeActors = activeActors;

        // Create executor and child actors list
        this.executor = Executors.newCachedThreadPool();
        this.childActors = Collections.synchronizedList(new ArrayList<>());
        this.isShuttingDown = false;
    }

    /**
     * Recursively crawls a URL and spawns child actors for discovered links.
     *
     * @param url the URL to crawl
     * @param depth the current depth in the crawl tree
     * @return CompletableFuture that completes with the final CrawlResult
     */
    public CompletableFuture<CrawlResult> crawlRecursively(String url, int depth) {
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Process this URL asynchronously using CompletableFuture
                processUrl(url, depth);

                // Create final result with all pages from shared collections
                long endTime = System.currentTimeMillis();
                return new CrawlResult(
                    new ArrayList<>(successfulPages),
                    new ArrayList<>(failedUrls),
                    startTime,
                    endTime
                );

            } catch (Exception e) {
                throw new RuntimeException("Recursive crawling failed: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Processes a single URL and spawns child actors for discovered links.
     *
     * @param url the URL to process
     * @param depth the current depth in the crawl tree
     */
    private void processUrl(String url, int depth) {
        // Check termination conditions
        if (depth > maxDepth || pagesCrawled.get() >= maxPages || isShuttingDown) {
            return;
        }

        // Check if URL was already visited
        String normalizedUrl = Page.normalizeUrl(url);
        if (visitedUrls.putIfAbsent(normalizedUrl, true) != null) {
            return;
        }

        // Check page limit again after marking as visited
        if (pagesCrawled.get() >= maxPages) {
            return;
        }

        // Try to fetch the page using Page.fromUrl
        try {
            Page page = Page.fromUrl(url, timeoutMs);

            // Add page to shared collections
            successfulPages.add(page);
            int currentCount = pagesCrawled.incrementAndGet();

            // Check if we've reached the page limit
            if (currentCount >= maxPages) {
                return;
            }

            // Process discovered links if within depth limit
            if (depth < maxDepth) {
                List<CompletableFuture<CrawlResult>> childFutures = new ArrayList<>();

                for (String link : page.links()) {
                    // Check page limit before creating child actors
                    if (pagesCrawled.get() >= maxPages) {
                        break;
                    }

                    if (shouldFollowLink(link)) {
                        String normalizedLink = Page.normalizeUrl(link);
                        // Don't mark as visited here - let the child actor do it
                        if (!visitedUrls.containsKey(normalizedLink)) {
                            // Create child actor with shared state
                            RecursiveActor childActor = new RecursiveActor(
                                maxDepth, maxPages, timeoutMs, followExternalLinks, startDomain, maxActors,
                                visitedUrls, successfulPages, failedUrls, pagesCrawled, activeActors
                            );
                            childActors.add(childActor);
                            activeActors.incrementAndGet();

                            // Start child actor recursively
                            CompletableFuture<CrawlResult> childFuture = childActor.crawlRecursively(link, depth + 1);
                            childFutures.add(childFuture);

                            // Handle child completion
                            childFuture.whenComplete((result, throwable) -> {
                                activeActors.decrementAndGet();
                                if (throwable != null) {
                                    failedUrls.add(link);
                                }
                                // Results are already merged through shared collections
                            });
                        }
                    }
                }

                // Wait for all child actors to complete
                if (!childFutures.isEmpty()) {
                    CompletableFuture.allOf(childFutures.toArray(new CompletableFuture[0])).join();
                }
            }

        } catch (IOException e) {
            // Handle fetch failure
            failedUrls.add(url);
        }
    }


    /**
     * Determines if a link should be followed based on crawler configuration.
     *
     * @param url the URL to check
     * @return true if the link should be followed
     */
    private boolean shouldFollowLink(String url) {
        if (followExternalLinks) {
            return true;
        }
        // Only follow links from the same domain
        return url.contains(startDomain);
    }

    /**
     * Gets the current partial results from the actor.
     * This is useful when a timeout occurs and we want to return what was crawled so far.
     *
     * @return CrawlResult with current state
     */
    public CrawlResult getPartialResults() {
        return new CrawlResult(
            new ArrayList<>(successfulPages),
            new ArrayList<>(failedUrls),
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );
    }

    /**
     * Shuts down the actor and all child actors.
     */
    public void shutdown() {
        isShuttingDown = true;

        // Shutdown all child actors
        childActors.forEach(RecursiveActor::shutdown);

        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

}
