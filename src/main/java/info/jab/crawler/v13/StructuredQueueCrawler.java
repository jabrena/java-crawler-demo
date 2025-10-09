package info.jab.crawler.v13;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.UrlDepthPair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.StructuredTaskScope;

/**
 * A structured concurrency-based web crawler using BlockingQueue + worker pattern.
 *
 * This crawler combines the proven producer-consumer pattern from V2 with Java 25's
 * StructuredTaskScope for automatic resource management and virtual threads.
 *
 * Design characteristics:
 * - BlockingQueue for URL frontier management
 * - StructuredTaskScope for worker task coordination
 * - Virtual threads for efficient concurrency
 * - Thread-safe data structures (ConcurrentHashMap, AtomicInteger)
 * - Parallel page fetching and processing
 * - Maintains a visited set to avoid duplicates across threads
 * - Respects maximum depth and page limits
 * - Automatic resource cleanup without poison pill pattern
 */
public class StructuredQueueCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int numThreads;

    public StructuredQueueCrawler(int maxDepth, int maxPages, int timeoutMs, boolean followExternalLinks, String startDomain, int numThreads) {
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
        // Thread-safe collections
        BlockingQueue<UrlDepthPair> urlQueue = new LinkedBlockingQueue<>();
        Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
        List<Page> successfulPages = Collections.synchronizedList(new ArrayList<>());
        List<String> failedUrls = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger pagesCrawled = new AtomicInteger(0);
        AtomicInteger activeWorkers = new AtomicInteger(0);
        AtomicBoolean shouldStop = new AtomicBoolean(false);

        long startTime = System.currentTimeMillis();

        // Initialize with seed URL at depth 0
        urlQueue.offer(new UrlDepthPair(seedUrl, 0));
        visitedUrls.add(Page.normalizeUrl(seedUrl));

        try (var scope = StructuredTaskScope.<Void>open()) {
            // Fork worker tasks
            for (int i = 0; i < numThreads; i++) {
                scope.fork(() -> {
                    while (!shouldStop.get()) {
                        try {
                            // Poll with timeout to check for completion periodically
                            UrlDepthPair current = urlQueue.poll(100, TimeUnit.MILLISECONDS);

                            if (current == null) {
                                // No work available, check if we should continue
                                if (urlQueue.isEmpty() && activeWorkers.get() == 0) {
                                    // No work and no active workers - we're done
                                    shouldStop.set(true);
                                    break;
                                }
                                continue;
                            }

                            // Check if we should stop before processing
                            if (shouldStop.get()) {
                                break;
                            }

                            activeWorkers.incrementAndGet();

                            // Check if we've reached the page limit atomically
                            if (pagesCrawled.get() >= maxPages) {
                                shouldStop.set(true);
                                activeWorkers.decrementAndGet();
                                break;
                            }

                            String url = current.url();
                            int depth = current.depth();

                            try {
                                // Create Page object using static factory method
                                Page page = Page.fromUrl(url, timeoutMs);

                                // Only increment if we haven't exceeded the limit
                                int newCount = pagesCrawled.incrementAndGet();
                                if (newCount > maxPages) {
                                    // We exceeded the limit, don't add the page
                                    pagesCrawled.decrementAndGet();
                                    shouldStop.set(true);
                                    activeWorkers.decrementAndGet();
                                    break;
                                }
                                successfulPages.add(page);

                                // Add new links to queue if within depth limit
                                if (depth < maxDepth && pagesCrawled.get() < maxPages) {
                                    page.links().stream()
                                        .filter(this::shouldFollowLink)
                                        .filter(link -> {
                                            String normalized = Page.normalizeUrl(link);
                                            return visitedUrls.add(normalized); // Thread-safe add and check
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
            }

            // Wait for all worker tasks to complete
            try {
                scope.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Crawling was interrupted", e);
            }
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
