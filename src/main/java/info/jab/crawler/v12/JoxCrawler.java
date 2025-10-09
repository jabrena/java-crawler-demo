package info.jab.crawler.v12;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;

import com.softwaremill.jox.structured.CancellableFork;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.softwaremill.jox.structured.Scopes.supervised;

/**
 * Jox-based Structured Concurrency Crawler (v12) using SoftwareMill's Jox library.
 *
 * This crawler implements structured concurrency using Jox's supervised scopes,
 * addressing the SoftwareMill critique of JEP 505 by providing:
 *
 * 1. **Supervised Scopes**: Scope body runs in separate virtual thread with automatic supervision
 * 2. **Cancellable Forks**: Individual task cancellation support with `forkCancellable()`
 * 3. **Built-in Timeout**: Uses Jox's timeout mechanisms for page fetching
 * 4. **Uniform Cancellation**: Scope body participates in error handling like subtasks
 * 5. **Clear Semantics**: `Fork.join()` provides clear blocking behavior (unlike Subtask.get())
 *
 * Key advantages over JEP 505 StructuredTaskScope:
 * - Supervisor pattern with separate virtual thread for scope body
 * - Better error handling and automatic cleanup
 * - Individual task cancellation support
 * - Clear fork semantics without confusion with Future.get()
 * - Addresses all major critiques from the SoftwareMill article
 *
 * Architecture:
 * - Uses `supervised()` to create supervised scopes
 * - Uses `forkCancellable()` for individual crawl tasks
 * - Recursive crawling with natural tree structure
 * - Thread-safe collections for coordination
 * - Automatic interruption and cleanup by supervisor
 */
public class JoxCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;

    /**
     * Creates a new Jox-based structured concurrency crawler.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     */
    public JoxCrawler(int maxDepth, int maxPages, int timeoutMs,
                     boolean followExternalLinks, String startDomain) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
    }

    /**
     * Crawls the web starting from the given seed URL using Jox supervised scopes.
     *
     * This implementation uses Jox's supervised scopes to provide:
     * - Automatic supervision and cleanup
     * - Individual task cancellation support
     * - Clear fork semantics with Fork.join()
     * - Supervisor pattern with separate virtual thread for scope body
     * - Better error handling than JEP 505 StructuredTaskScope
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
            // Use Jox supervised scope for main crawling
            supervised(scope -> {
                // Fork the main crawling task with cancellation support
                var mainTask = scope.forkCancellable(() -> {
                    crawlWithJoxSupervisedScope(
                        new CrawlTask(seedUrl, 0),
                        visitedUrls,
                        pagesCrawled,
                        successfulPages,
                        failedUrls
                    );
                    return null; // Return value required by forkCancellable
                });

                // Wait for completion with clear semantics
                mainTask.join();
                return null; // Return value required by supervised scope
            });
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
     * Recursively crawls using Jox supervised scopes with cancellable forks.
     *
     * This method demonstrates the key Jox features:
     * 1. Supervised scopes with automatic supervision and cleanup
     * 2. Cancellable forks for individual task cancellation
     * 3. Clear fork semantics with Fork.join()
     * 4. Supervisor pattern with separate virtual thread for scope body
     * 5. Better error handling than JEP 505 StructuredTaskScope
     *
     * @param task the crawl task to process
     * @param visitedUrls thread-safe set of visited URLs
     * @param pagesCrawled atomic counter for pages crawled
     * @param successfulPages thread-safe list of successful pages
     * @param failedUrls thread-safe list of failed URLs
     */
    private void crawlWithJoxSupervisedScope(
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
            // Fetch and parse the page
            Page page = Page.fromUrl(task.url(), timeoutMs);

            // Check page limit after successful fetch (scope body participation)
            int currentCount = pagesCrawled.incrementAndGet();
            if (currentCount > maxPages) {
                pagesCrawled.decrementAndGet();
                return; // Scope body signals completion
            }

            successfulPages.add(page);

            // Process discovered links if within limits
            if (task.isWithinDepthLimit(maxDepth) && !page.links().isEmpty()) {
                // Create child supervised scope for parallel processing
                try {
                    supervised(childScope -> {
                        List<CancellableFork<Object>> childTasks = new ArrayList<>();

                        for (String link : page.links()) {
                            // Check limits before forking (scope body participation)
                            if (pagesCrawled.get() >= maxPages) {
                                break; // Scope body signals completion
                            }

                            if (shouldFollowLink(link)) {
                                String normalizedLink = Page.normalizeUrl(link);
                                if (!visitedUrls.containsKey(normalizedLink)) {
                                    CrawlTask childTask = task.nextDepth();

                                    // Fork child task with cancellation support
                                    var childTaskHandle = childScope.forkCancellable(() -> {
                                        crawlWithJoxSupervisedScope(
                                            new CrawlTask(link, childTask.depth()),
                                            visitedUrls, pagesCrawled,
                                            successfulPages, failedUrls
                                        );
                                        return null; // Return value required by forkCancellable
                                    });
                                    childTasks.add(childTaskHandle);
                                }
                            }
                        }

                        // Wait for all child tasks with clear semantics
                        // Jox automatically handles cancellation and cleanup
                        for (var childTask : childTasks) {
                            childTask.join();
                        }
                        return null; // Return value required by supervised scope
                    });
                } catch (Exception e) {
                    // Handle child scope errors
                    failedUrls.add(task.url() + " (child scope error: " + e.getMessage() + ")");
                }
            }

        } catch (Exception e) {
            // Handle page fetch errors
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
