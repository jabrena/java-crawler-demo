package info.jab.crawler.v8;

import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import info.jab.crawler.v6.ActorMessage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.StructuredTaskScope;

/**
 * Supervisor actor that coordinates crawling using structural concurrency.
 *
 * This actor is responsible for:
 * - Managing shared state (visited URLs, results, counters)
 * - Coordinating crawling tasks using structural concurrency
 * - Processing messages for state updates and coordination
 * - Handling fault tolerance and error recovery
 * - Managing resource limits and completion detection
 *
 * The supervisor uses structural concurrency for actual crawling work while
 * maintaining actor-based coordination and state management.
 */
public class SupervisorActor {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int maxConcurrentTasks;

    // Thread-safe collections for state management
    private final ConcurrentHashMap<String, Boolean> visitedUrls;
    private final List<Page> successfulPages;
    private final List<String> failedUrls;
    private final AtomicInteger pagesCrawled;
    private final AtomicInteger activeTasks;

    // Message processing
    private final BlockingQueue<ActorMessage> messageQueue;
    private final ExecutorService supervisorExecutor;
    private volatile boolean isShuttingDown;

    /**
     * Creates a new supervisor actor.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     * @param maxConcurrentTasks maximum number of concurrent crawling tasks
     */
    public SupervisorActor(int maxDepth, int maxPages, int timeoutMs,
                          boolean followExternalLinks, String startDomain, int maxConcurrentTasks) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
        this.maxConcurrentTasks = maxConcurrentTasks;

        // Initialize thread-safe collections
        this.visitedUrls = new ConcurrentHashMap<>();
        this.successfulPages = Collections.synchronizedList(new ArrayList<>());
        this.failedUrls = Collections.synchronizedList(new ArrayList<>());
        this.pagesCrawled = new AtomicInteger(0);
        this.activeTasks = new AtomicInteger(0);

        // Initialize message processing
        this.messageQueue = new LinkedBlockingQueue<>();
        this.supervisorExecutor = Executors.newSingleThreadExecutor();
        this.isShuttingDown = false;
    }

    /**
     * Crawls using structural concurrency with actor-based state management.
     *
     * @param seedUrl the starting URL for the crawl
     * @param startTime the start time of the crawl
     * @return CrawlResult containing all crawled pages and statistics
     */
    public CrawlResult crawlWithStructuralConcurrency(String seedUrl, long startTime) {
        try {
            // Use structural concurrency for the actual crawling work
            crawlWithStructuralConcurrency(seedUrl, 0);

            // Create final result
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
     * Recursively crawls using structural concurrency while maintaining actor-based state management.
     *
     * @param url the URL to crawl
     * @param depth the current depth in the crawl tree
     */
    private void crawlWithStructuralConcurrency(String url, int depth) {
        // Check termination conditions
        if (depth > maxDepth || pagesCrawled.get() >= maxPages || isShuttingDown) {
            return;
        }

        // Check if URL was already visited (actor state management)
        String normalizedUrl = normalizeUrl(url);
        if (visitedUrls.putIfAbsent(normalizedUrl, true) != null) {
            return;
        }

        // Check page limit again after marking as visited
        if (pagesCrawled.get() >= maxPages) {
            return;
        }

        try {
            // Fetch and parse the page directly (no need for structural concurrency for single page)
            Document doc = fetchAndParsePage(url);
            Page page = createPage(url, doc);
            List<String> links = extractLinks(doc);

            // Update actor state
            successfulPages.add(page);
            int currentCount = pagesCrawled.incrementAndGet();

            // Check if we've reached the page limit
            if (currentCount >= maxPages) {
                return;
            }

            // Process discovered links if within depth limit
            if (depth < maxDepth && !links.isEmpty()) {
                // Create a new structured scope for child crawls
                try (var childScope = new StructuredTaskScope<Void>()) {
                    List<StructuredTaskScope.Subtask<Void>> childTasks = new ArrayList<>();

                    for (String link : links) {
                        // Check page limit before creating child tasks
                        if (pagesCrawled.get() >= maxPages) {
                            break;
                        }

                        if (shouldFollowLink(link)) {
                            String normalizedLink = normalizeUrl(link);
                            // Don't mark as visited here - let the child task do it
                            if (!visitedUrls.containsKey(normalizedLink)) {
                                // Fork child crawl as a subtask
                                var childTask = childScope.fork(() -> {
                                    crawlWithStructuralConcurrency(link, depth + 1);
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
        }
    }

    /**
     * Fetches and parses a web page.
     *
     * @param url the URL to fetch
     * @return the parsed document
     * @throws IOException if the page cannot be fetched
     */
    private Document fetchAndParsePage(String url) throws IOException {
        return Jsoup.connect(url)
            .timeout(timeoutMs)
            .userAgent("Mozilla/5.0 (Educational Hybrid Actor-Structural Crawler)")
            .maxBodySize(1024 * 1024) // Limit body size to 1MB for performance
            .get();
    }

    /**
     * Creates a Page object from a parsed document.
     *
     * @param url the original URL
     * @param doc the parsed document
     * @return a Page object with extracted content
     */
    private Page createPage(String url, Document doc) {
        String title = doc.title();
        String content = doc.body().text();
        List<String> links = extractLinks(doc);

        return new Page(url, title, 200, content, links);
    }

    /**
     * Extracts all absolute links from a document.
     *
     * @param doc the document to extract links from
     * @return list of absolute URLs
     */
    private List<String> extractLinks(Document doc) {
        return doc.select("a[href]")
            .stream()
            .map(element -> element.absUrl("href"))
            .filter(link -> !link.isEmpty())
            .filter(link -> link.startsWith("http://") || link.startsWith("https://"))
            .filter(link -> !link.contains("#")) // Exclude fragment-only links
            .filter(this::shouldFollowLink)
            .limit(20) // Limit links per page for performance
            .toList();
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
     * Normalizes a URL by removing fragments and trailing slashes.
     *
     * @param url the URL to normalize
     * @return normalized URL
     */
    private String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

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
     * Shuts down the supervisor actor.
     */
    public void shutdown() {
        isShuttingDown = true;

        // Shutdown executor
        supervisorExecutor.shutdown();
        try {
            if (!supervisorExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                supervisorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            supervisorExecutor.shutdownNow();
        }
    }
}
