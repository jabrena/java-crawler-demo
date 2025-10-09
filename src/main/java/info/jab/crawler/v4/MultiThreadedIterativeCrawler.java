package info.jab.crawler.v4;

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
 * A multi-threaded iterative web crawler using producer-consumer pattern.
 *
 * Design characteristics:
 * - Multi-threaded execution using ExecutorService for parallel processing
 * - Iterative approach with thread-safe coordination using BlockingQueue
 * - Breadth-first traversal with parallel URL processing
 * - Maintains a visited set to avoid duplicates across threads
 * - Respects maximum depth and page limits
 * - Uses producer-consumer pattern with worker threads (similar to V2)
 * - No recursion or trampoline pattern - uses iterative while loops
 */
public class MultiThreadedIterativeCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int numThreads;

    public MultiThreadedIterativeCrawler(int maxDepth, int maxPages, int timeoutMs, boolean followExternalLinks, String startDomain, int numThreads) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
        this.numThreads = numThreads;
    }

    /**
     * Crawls the web starting from the given seed URL using multi-threaded iterative approach.
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

        // Create thread pool for parallel execution
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // Create a poison pill to signal shutdown
        final UrlDepthPair POISON_PILL = UrlDepthPair.poisonPill();

        // Submit worker tasks
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                while (true) {
                    try {
                        // Poll with timeout to check for completion periodically
                        UrlDepthPair current = urlQueue.poll(100, TimeUnit.MILLISECONDS);

                        if (current == null) {
                            // No work available, check if we should continue
                            if (activeWorkers.get() == 0 && urlQueue.isEmpty()) {
                                // No active workers and no pending work - we're done
                                urlQueue.offer(POISON_PILL); // Signal other threads
                                break;
                            }
                            continue;
                        }

                        // Check for poison pill
                        if (current.isPoisonPill()) {
                            urlQueue.offer(POISON_PILL); // Pass it on to other threads
                            break;
                        }

                        activeWorkers.incrementAndGet();

                        // Check if we've reached the page limit atomically
                        int currentCount = pagesCrawled.get();
                        if (currentCount >= maxPages) {
                            activeWorkers.decrementAndGet();
                            urlQueue.offer(POISON_PILL);
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
                                urlQueue.offer(POISON_PILL);
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
            });
        }

        // Shutdown executor and wait for completion
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
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
