package info.jab.crawler.v4;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
            visitedUrls.put(normalizeUrl(seedUrl), true);
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
        final UrlDepthPair POISON_PILL = new UrlDepthPair(null, -1);

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
                        if (current.url() == null) {
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
                            // Fetch and parse the page
                            Document doc = Jsoup.connect(url)
                                .timeout(timeoutMs)
                                .userAgent("Mozilla/5.0 (Educational Crawler)")
                                .maxBodySize(1024 * 1024) // Limit body size to 1MB for performance
                                .get();

                            // Extract information
                            String title = doc.title();
                            String content = doc.body().text();
                            List<String> links = extractLinks(doc);

                            // Only increment if we haven't exceeded the limit
                            int newCount = pagesCrawled.incrementAndGet();
                            if (newCount > maxPages) {
                                // We exceeded the limit, don't add the page
                                pagesCrawled.decrementAndGet();
                                activeWorkers.decrementAndGet();
                                urlQueue.offer(POISON_PILL);
                                break;
                            }

                            // Create Page object and add to results
                            Page page = new Page(url, title, 200, content, links);
                            successfulPages.add(page);

                            // Add new links to queue if within depth limit
                            if (depth < maxDepth && pagesCrawled.get() < maxPages) {
                                links.stream()
                                    .filter(this::shouldFollowLink)
                                    .filter(link -> {
                                        String normalized = normalizeUrl(link);
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
     * Extracts all absolute links from a document.
     * Returns an immutable list following functional programming principles.
     */
    private List<String> extractLinks(Document doc) {
        return doc.select("a[href]")
            .stream()
            .map(element -> element.absUrl("href"))
            .filter(link -> !link.isEmpty())
            .filter(link -> link.startsWith("http://") || link.startsWith("https://"))
            .limit(20) // Limit links per page for performance
            .toList();
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

    /**
     * Normalizes a URL by removing fragments and trailing slashes.
     * This is a pure function with no side effects.
     *
     * @param url the URL to normalize
     * @return normalized URL
     */
    private String normalizeUrl(String url) {
        // Handle null or empty URLs
        if (url == null || url.isEmpty()) {
            return url;
        }

        // More efficient normalization
        int hashIndex = url.indexOf('#');
        if (hashIndex != -1) {
            url = url.substring(0, hashIndex);
        }
        if (url.endsWith("/") && url.length() > 1) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Internal record to track URL and its depth in the crawl tree.
     */
    private record UrlDepthPair(String url, int depth) {}
}
