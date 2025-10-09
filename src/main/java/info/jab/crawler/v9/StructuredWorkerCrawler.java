package info.jab.crawler.v9;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.UrlDepthPair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A structured concurrency-based web crawler using Java 25's StructuredTaskScope
 * combined with the proven BlockingQueue + worker pattern from MultiThreadedIterativeCrawler.
 *
 * Design characteristics:
 * - Uses StructuredTaskScope for automatic resource management and cleanup
 * - BlockingQueue-based coordination for URL distribution among workers
 * - Virtual threads for efficient concurrency without thread pool overhead
 * - Breadth-first traversal with parallel URL processing
 * - Maintains a visited set to avoid duplicates across workers
 * - Respects maximum depth and page limits
 * - Automatic cancellation and cleanup when scope closes
 * - Better error propagation through structured scoping
 *
 * Key advantages over v4 (MultiThreadedIterativeCrawler):
 * - No ExecutorService shutdown complexity
 * - No poison pill pattern needed (use scope cancellation)
 * - Automatic worker cleanup on scope exit
 * - Better exception handling through structured tasks
 * - Virtual threads for better resource utilization
 *
 * Key advantages over v7 (StructuralConcurrencyCrawler):
 * - Uses proven queue-based worker pattern instead of recursive forking
 * - Better throughput for many URLs due to worker pool approach
 * - More predictable resource usage patterns
 */
public class StructuredWorkerCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int numThreads;

    public StructuredWorkerCrawler(int maxDepth, int maxPages, int timeoutMs, boolean followExternalLinks, String startDomain, int numThreads) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
        this.numThreads = numThreads;
    }

    /**
     * Crawls the web starting from the given seed URL using structured concurrency.
     *
     * @param seedUrl the starting URL for the crawl
     * @return CrawlResult containing all crawled pages and statistics
     */
    @Override
    public CrawlResult crawl(String seedUrl) {
        // Thread-safe collections for coordination
        BlockingQueue<UrlDepthPair> urlQueue = new LinkedBlockingQueue<>();
        ConcurrentHashMap<String, Boolean> visitedUrls = new ConcurrentHashMap<>();
        AtomicInteger pagesCrawled = new AtomicInteger(0);
        List<Page> successfulPages = Collections.synchronizedList(new ArrayList<>());
        List<String> failedUrls = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger activeWorkers = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Initialize with seed URL at depth 0
        if (seedUrl != null && !seedUrl.isEmpty()) {
            urlQueue.offer(new UrlDepthPair(seedUrl, 0));
            visitedUrls.put(Page.normalizeUrl(seedUrl), true);
        } else {
            // Handle invalid seed URL
            if (seedUrl != null) {
                failedUrls.add(seedUrl);
            } else {
                failedUrls.add("null");
            }
        }

        try {
            // Create structured scope for the entire crawl operation
            try (var scope = StructuredTaskScope.<Void>open()) {
                // Create worker tasks using structured concurrency
                List<StructuredTaskScope.Subtask<Void>> workerTasks = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                    var workerTask = scope.fork(() -> {
                        while (true) {
                            try {
                                // Poll with timeout to check for completion periodically
                                UrlDepthPair current = urlQueue.poll(100, TimeUnit.MILLISECONDS);

                                if (current == null) {
                                    // No work available, check if we should continue
                                    if (pagesCrawled.get() >= maxPages) {
                                        break;
                                    }
                                    // If no active workers and queue is empty, we're done
                                    if (activeWorkers.get() == 0 && urlQueue.isEmpty()) {
                                        break;
                                    }
                                    continue;
                                }

                                // Check for poison pill (null URL)
                                if (current.url() == null) {
                                    break;
                                }

                                activeWorkers.incrementAndGet();

                                // Check if we've reached the page limit atomically
                                int currentCount = pagesCrawled.get();
                                if (currentCount >= maxPages) {
                                    activeWorkers.decrementAndGet();
                                    break;
                                }

                                String url = current.url();
                                int depth = current.depth();

                                try {
                                    // Fetch and parse the page using Page.fromUrl
                                    Page page = Page.fromUrl(url, timeoutMs);

                                    // Only increment if we haven't exceeded the limit
                                    int newCount = pagesCrawled.incrementAndGet();
                                    if (newCount > maxPages) {
                                        // We exceeded the limit, don't add the page
                                        pagesCrawled.decrementAndGet();
                                        activeWorkers.decrementAndGet();
                                        break;
                                    }

                                    // Add page to results
                                    successfulPages.add(page);

                                    // Add new links to queue if within depth limit
                                    if (depth < maxDepth && pagesCrawled.get() < maxPages) {
                                        page.links().stream()
                                            .filter(this::shouldFollowLink)
                                            .filter(link -> {
                                                String normalized = Page.normalizeUrl(link);
                                                return visitedUrls.putIfAbsent(normalized, true) == null; // Thread-safe add and check
                                            })
                                            .forEach(link -> urlQueue.offer(new UrlDepthPair(link, depth + 1)));
                                    }

                                } catch (IOException e) {
                                    failedUrls.add(url);
                                } finally {
                                    activeWorkers.decrementAndGet();
                                }

                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                        return null;
                    });
                    workerTasks.add(workerTask);
                }

                // Wait for all worker tasks to complete
                scope.join();

                // Check for failures in worker tasks
                for (var workerTask : workerTasks) {
                    if (workerTask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                        // Log the failure but continue with other tasks
                        System.err.println("Worker task failed: " + workerTask.exception().getMessage());
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Structured worker crawling failed: " + e.getMessage(), e);
        }

        long endTime = System.currentTimeMillis();

        return new CrawlResult(successfulPages, failedUrls, startTime, endTime);
    }


    /**
     * Determines if a link should be followed based on crawler configuration.
     */
    private boolean shouldFollowLink(String url) {
        if (followExternalLinks) {
            return true;
        }
        // Only follow links from the same domain
        return url.contains(startDomain);
    }


}
