package info.jab.crawler.v9;

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
            visitedUrls.put(normalizeUrl(seedUrl), true);
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
                                    // Fetch and parse the page
                                    Document doc = Jsoup.connect(url)
                                        .timeout(timeoutMs)
                                        .userAgent("Mozilla/5.0 (Educational Structured Worker Crawler)")
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
