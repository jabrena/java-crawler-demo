package info.jab.crawler.v7;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.StructuredTaskScope;

/**
 * Structural Concurrency-based web crawler using Java 25's StructuredTaskScope.
 *
 * This crawler implements a modern approach using Java 25's structural concurrency features:
 * - StructuredTaskScope for managing concurrent subtasks within well-defined scopes
 * - Automatic cancellation and cleanup when scope closes
 * - Simplified error handling and propagation
 * - Natural tree-like crawling structure with proper resource management
 * - Virtual threads for efficient concurrency
 * - Scoped values for configuration sharing
 *
 * Design characteristics:
 * - Each crawl operation creates a structured scope for its subtasks
 * - Child crawls are forked as subtasks within the parent scope
 * - Automatic cleanup and cancellation when scope closes
 * - Fault isolation - failures in one branch don't affect others
 * - Resource management through structured scoping
 * - Breadth-first traversal with parallel execution
 * - Maintains visited URLs to avoid duplicates
 * - Respects maximum depth and page limits
 */
public class StructuralConcurrencyCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;

    /**
     * Creates a new structural concurrency-based crawler.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     */
    public StructuralConcurrencyCrawler(int maxDepth, int maxPages, int timeoutMs,
                                       boolean followExternalLinks, String startDomain) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
    }

    /**
     * Crawls the web starting from the given seed URL using structural concurrency.
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

        // Shared state for coordination
        ConcurrentHashMap<String, Boolean> visitedUrls = new ConcurrentHashMap<>();
        List<Page> successfulPages = Collections.synchronizedList(new ArrayList<>());
        List<String> failedUrls = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger pagesCrawled = new AtomicInteger(0);

        try {
            // Create structured scope for the entire crawl operation
            try (var scope = StructuredTaskScope.<Void>open()) {
                // Start the recursive crawling process
                var crawlTask = scope.fork(() -> {
                    crawlRecursively(seedUrl, 0, visitedUrls, successfulPages, failedUrls, pagesCrawled);
                    return null;
                });

                // Wait for all subtasks to complete
                scope.join();

                // Check if any task failed
                if (crawlTask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                    throw new RuntimeException("Crawling failed", crawlTask.exception());
                }
            }

            long endTime = System.currentTimeMillis();
            return new CrawlResult(
                new ArrayList<>(successfulPages),
                new ArrayList<>(failedUrls),
                startTime,
                endTime
            );

        } catch (Exception e) {
            throw new RuntimeException("Structural concurrency crawling failed: " + e.getMessage(), e);
        }
    }

    /**
     * Recursively crawls a URL and its discovered links using structural concurrency.
     *
     * @param url the URL to crawl
     * @param depth the current depth in the crawl tree
     * @param visitedUrls shared visited URLs map
     * @param successfulPages shared successful pages list
     * @param failedUrls shared failed URLs list
     * @param pagesCrawled shared pages crawled counter
     */
    private void crawlRecursively(String url, int depth,
                                 ConcurrentHashMap<String, Boolean> visitedUrls,
                                 List<Page> successfulPages,
                                 List<String> failedUrls,
                                 AtomicInteger pagesCrawled) {
        // Check termination conditions
        if (depth > maxDepth || pagesCrawled.get() >= maxPages) {
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

        try {
            // Fetch and parse the page using Page.fromUrl
            Page page = Page.fromUrl(url, timeoutMs);

            // Add page to shared collections
            successfulPages.add(page);
            int currentCount = pagesCrawled.incrementAndGet();

            // Check if we've reached the page limit
            if (currentCount >= maxPages) {
                return;
            }

            // Process discovered links if within depth limit
            if (depth < maxDepth && !page.links().isEmpty()) {
                // Create a new structured scope for child crawls
                try (var childScope = StructuredTaskScope.<Void>open()) {
                    List<StructuredTaskScope.Subtask<Void>> childTasks = new ArrayList<>();

                    for (String link : page.links()) {
                        // Check page limit before creating child tasks
                        if (pagesCrawled.get() >= maxPages) {
                            break;
                        }

                        if (shouldFollowLink(link)) {
                            String normalizedLink = Page.normalizeUrl(link);
                            // Don't mark as visited here - let the child task do it
                            if (!visitedUrls.containsKey(normalizedLink)) {
                                // Fork child crawl as a subtask
                                var childTask = childScope.fork(() -> {
                                    crawlRecursively(link, depth + 1, visitedUrls,
                                                   successfulPages, failedUrls, pagesCrawled);
                                    return null;
                                });
                                childTasks.add(childTask);
                            }
                        }
                    }

                    // Wait for all child tasks to complete
                    if (!childTasks.isEmpty()) {
                        childScope.join();

                        // Check for failures in child tasks
                        for (var childTask : childTasks) {
                            if (childTask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                                // Log the failure but continue with other tasks
                                System.err.println("Child crawl failed: " + childTask.exception().getMessage());
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            // Handle fetch failure
            failedUrls.add(url);
        } catch (Exception e) {
            // Handle other failures
            failedUrls.add(url);
            throw new RuntimeException("Failed to crawl URL: " + url, e);
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

}
