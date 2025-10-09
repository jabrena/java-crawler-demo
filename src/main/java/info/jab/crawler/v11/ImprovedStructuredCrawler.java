package info.jab.crawler.v11;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Improved Structured Concurrency Crawler (v11) addressing SoftwareMill critique of JEP 505.
 *
 * This crawler implements the following improvements over standard StructuredTaskScope:
 *
 * 1. **Uniform Cancellation**: Scope body participates in error handling like subtasks
 * 2. **Unified Scope Logic**: No split between Joiner implementation and scope body
 * 3. **Timeout as Method**: Lightweight timeout pattern without configuration parameter
 * 4. **Custom Joiner**: Race semantics for better control over completion logic
 *
 * Key design decisions:
 * - Uses UnifiedCancellationJoiner for race semantics and unified cancellation
 * - Implements timeout-as-method pattern using TimeoutUtil
 * - Scope body can signal completion to cancel remaining subtasks
 * - All tasks participate in error handling uniformly
 * - Natural tree-like crawling structure with proper resource management
 *
 * This addresses the main critiques from the SoftwareMill article:
 * - Non-uniform cancellation (scope body now participates)
 * - Scope logic split (unified in custom Joiner)
 * - Redundant timeout configuration (implemented as method)
 * - Better control over completion semantics
 */
public class ImprovedStructuredCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;

    /**
     * Creates a new improved structured concurrency crawler.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     */
    public ImprovedStructuredCrawler(int maxDepth, int maxPages, int timeoutMs,
                                   boolean followExternalLinks, String startDomain) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
    }

    /**
     * Crawls the web starting from the given seed URL using improved structured concurrency.
     *
     * This implementation addresses the SoftwareMill critique by:
     * 1. Using custom UnifiedCancellationJoiner for race semantics
     * 2. Implementing timeout-as-method pattern
     * 3. Allowing scope body to participate in cancellation
     * 4. Providing unified scope logic without Joiner/body split
     *
     * @param seedUrl the starting URL for the crawl
     * @return CrawlResult containing all crawled pages and statistics
     */
    @Override
    public CrawlResult crawl(String seedUrl) {
        if (seedUrl == null || seedUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Seed URL cannot be null or empty");
        }

        long startTime = System.currentTimeMillis();

        // Thread-safe collections for coordination
        ConcurrentHashMap<String, Boolean> visitedUrls = new ConcurrentHashMap<>();
        AtomicInteger pagesCrawled = new AtomicInteger(0);
        List<Page> successfulPages = Collections.synchronizedList(new ArrayList<>());
        List<String> failedUrls = Collections.synchronizedList(new ArrayList<>());

        try {
            // Use improved structured concurrency with custom joiner for main scope
            try (var scope = StructuredTaskScope.<Void, Void>open(new UnifiedCancellationJoiner<>())) {
                scope.fork(() -> {
                    crawlWithImprovedStructuredConcurrency(
                        new CrawlTask(seedUrl, 0),
                        visitedUrls,
                        pagesCrawled,
                        successfulPages,
                        failedUrls
                    );
                    return (Void) null; // More explicit about the intent
                });

                // Wait for completion with race semantics
                scope.join();
            }
        } catch (Exception e) {
            // Handle any unexpected errors
            failedUrls.add(seedUrl + " (error: " + e.getMessage() + ")");
        }

        long endTime = System.currentTimeMillis();
        return new CrawlResult(
            new ArrayList<>(successfulPages),
            new ArrayList<>(failedUrls),
            startTime,
            endTime
        );
    }

    /**
     * Recursively crawls using improved structured concurrency with unified cancellation.
     *
     * This method demonstrates the key improvements:
     * 1. Custom UnifiedCancellationJoiner provides race semantics
     * 2. Scope body can signal completion to cancel remaining work
     * 3. Timeout-as-method pattern for individual page fetches
     * 4. Unified error handling across all tasks
     *
     * @param task the crawl task to process
     * @param visitedUrls thread-safe set of visited URLs
     * @param pagesCrawled atomic counter for pages crawled
     * @param successfulPages thread-safe list of successful pages
     * @param failedUrls thread-safe list of failed URLs
     */
    private void crawlWithImprovedStructuredConcurrency(
            CrawlTask task,
            ConcurrentHashMap<String, Boolean> visitedUrls,
            AtomicInteger pagesCrawled,
            List<Page> successfulPages,
            List<String> failedUrls) {

        // Check termination conditions early (scope body participation)
        if (pagesCrawled.get() >= maxPages) {
            return; // Scope body signals completion
        }

        if (task.isAtMaxDepth(maxDepth)) {
            return; // Scope body signals completion
        }

        String normalizedUrl = Page.normalizeUrl(task.url());
        if (visitedUrls.putIfAbsent(normalizedUrl, true) != null) {
            return; // Already visited, scope body signals completion
        }

        try {
            // Use timeout-as-method pattern for individual page fetch
            Page page = TimeoutUtil.timeout(
                Duration.ofMillis(timeoutMs),
                () -> Page.fromUrl(task.url(), timeoutMs)
            );

            // Check page limit after successful fetch (scope body participation)
            int currentCount = pagesCrawled.incrementAndGet();
            if (currentCount > maxPages) {
                pagesCrawled.decrementAndGet();
                return; // Scope body signals completion
            }

            successfulPages.add(page);

            // Process discovered links if within limits
            if (task.depth() < maxDepth && !page.links().isEmpty()) {
                // Create child scope with custom joiner for race semantics
                try (var scope = StructuredTaskScope.<Void, Void>open(new UnifiedCancellationJoiner<>())) {

                    // Fork child tasks for discovered links
                    List<StructuredTaskScope.Subtask<Void>> childTasks = new ArrayList<>();

                    for (String link : page.links()) {
                        // Check limits before forking (scope body participation)
                        if (pagesCrawled.get() >= maxPages) {
                            break; // Scope body signals completion
                        }

                        if (shouldFollowLink(link)) {
                            String normalizedLink = Page.normalizeUrl(link);
                            if (!visitedUrls.containsKey(normalizedLink)) {
                                CrawlTask childTask = new CrawlTask(link, task.depth() + 1);

                                var childTaskHandle = scope.fork(() -> {
                                    crawlWithImprovedStructuredConcurrency(
                                        childTask, visitedUrls, pagesCrawled,
                                        successfulPages, failedUrls
                                    );
                                    return null;
                                });
                                childTasks.add(childTaskHandle);
                            }
                        }
                    }

                    // Wait for all child tasks with race semantics
                    // The custom joiner allows scope body to cancel remaining work
                    scope.join();

                } catch (Exception e) {
                    // Handle child scope errors
                    failedUrls.add(task.url() + " (child scope error: " + e.getMessage() + ")");
                }
            }

        } catch (Exception e) {
            // Handle page fetch errors (including timeout)
            failedUrls.add(task.url() + " (error: " + e.getMessage() + ")");
        }
    }


    /**
     * Determines whether a link should be followed based on configuration.
     *
     * @param link the link to check
     * @return true if the link should be followed
     */
    private boolean shouldFollowLink(String link) {
        if (link == null || link.trim().isEmpty()) {
            return false;
        }

        try {
            URL url = new URL(link);
            String protocol = url.getProtocol();

            // Only follow HTTP/HTTPS links
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                return false;
            }

            // Check external link policy
            if (!followExternalLinks) {
                String host = url.getHost();
                return host != null && host.equals(startDomain);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
