package info.jab.crawler.v2;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A multi-threaded web crawler using the Producer-Consumer pattern.
 *
 * Design characteristics:
 * - Multi-threaded execution using ExecutorService
 * - Producer-Consumer pattern with BlockingQueue for URL frontier
 * - Thread-safe data structures (ConcurrentHashMap, AtomicInteger)
 * - Parallel page fetching and processing
 * - Maintains a visited set to avoid duplicates across threads
 * - Respects maximum depth and page limits
 */
public class ProducerConsumerCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int numThreads;

    private ProducerConsumerCrawler(Builder builder) {
        this.maxDepth = builder.maxDepth;
        this.maxPages = builder.maxPages;
        this.timeoutMs = builder.timeoutMs;
        this.followExternalLinks = builder.followExternalLinks;
        this.startDomain = builder.startDomain;
        this.numThreads = builder.numThreads;
    }

    /**
     * Crawls the web starting from the given seed URL using multiple threads.
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

        long startTime = System.currentTimeMillis();

        // Initialize with seed URL at depth 0
        urlQueue.offer(new UrlDepthPair(seedUrl, 0));
        visitedUrls.add(normalizeUrl(seedUrl));

        // Create thread pool
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
        // Remove fragment
        String normalized = url.split("#")[0];
        // Remove trailing slash using functional approach
        return normalized.endsWith("/")
            ? normalized.substring(0, normalized.length() - 1)
            : normalized;
    }

    /**
     * Internal record to track URL and its depth in the crawl tree.
     */
    private record UrlDepthPair(String url, int depth) {}

    /**
     * Builder for creating ProducerConsumerCrawler instances with custom configuration.
     * Follows immutable builder pattern with functional validation.
     */
    public static class Builder {
        private int maxDepth = 2;
        private int maxPages = 50;
        private int timeoutMs = 5000;
        private boolean followExternalLinks = false;
        private String startDomain = "";
        private int numThreads = 4;

        /**
         * Sets the maximum crawl depth.
         *
         * @param maxDepth maximum depth (must be non-negative)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if maxDepth is negative
         */
        public Builder maxDepth(int maxDepth) {
            validateNonNegative(maxDepth, "maxDepth");
            this.maxDepth = maxDepth;
            return this;
        }

        /**
         * Sets the maximum number of pages to crawl.
         *
         * @param maxPages maximum pages (must be positive)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if maxPages is not positive
         */
        public Builder maxPages(int maxPages) {
            validatePositive(maxPages, "maxPages");
            this.maxPages = maxPages;
            return this;
        }

        /**
         * Sets the connection timeout in milliseconds.
         *
         * @param timeoutMs timeout in milliseconds (must be positive)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if timeout is not positive
         */
        public Builder timeout(int timeoutMs) {
            validatePositive(timeoutMs, "timeout");
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets whether to follow external links.
         *
         * @param follow true to follow external links
         * @return this builder for method chaining
         */
        public Builder followExternalLinks(boolean follow) {
            this.followExternalLinks = follow;
            return this;
        }

        /**
         * Sets the starting domain for link filtering.
         *
         * @param domain the domain to restrict crawling to
         * @return this builder for method chaining
         */
        public Builder startDomain(String domain) {
            this.startDomain = domain;
            return this;
        }

        /**
         * Sets the number of worker threads.
         *
         * @param numThreads number of threads (must be positive)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if numThreads is not positive
         */
        public Builder numThreads(int numThreads) {
            validatePositive(numThreads, "numThreads");
            this.numThreads = numThreads;
            return this;
        }

        /**
         * Builds a new ProducerConsumerCrawler with the configured settings.
         *
         * @return a new ProducerConsumerCrawler instance
         */
        public ProducerConsumerCrawler build() {
            return new ProducerConsumerCrawler(this);
        }

        /**
         * Pure validation function for positive integers.
         */
        private void validatePositive(int value, String fieldName) {
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive");
            }
        }

        /**
         * Pure validation function for non-negative integers.
         */
        private void validateNonNegative(int value, String fieldName) {
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " must be non-negative");
            }
        }
    }
}

