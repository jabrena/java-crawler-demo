package info.jab.crawler.v1;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * A simple sequential web crawler that processes URLs one by one.
 *
 * Design characteristics:
 * - Single-threaded execution
 * - Iterative approach using a Queue for URL frontier
 * - Breadth-first traversal of web pages
 * - Maintains a visited set to avoid duplicates
 * - Respects maximum depth and page limits
 */
public class SequentialCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;

    private SequentialCrawler(Builder builder) {
        this.maxDepth = builder.maxDepth;
        this.maxPages = builder.maxPages;
        this.timeoutMs = builder.timeoutMs;
        this.followExternalLinks = builder.followExternalLinks;
        this.startDomain = builder.startDomain;
    }

    /**
     * Crawls the web starting from the given seed URL.
     *
     * @param seedUrl the starting URL for the crawl
     * @return CrawlResult containing all crawled pages and statistics
     */
    public CrawlResult crawl(String seedUrl) {
        CrawlResult result = CrawlResult.empty();
        Queue<UrlDepthPair> urlQueue = new ArrayDeque<>();
        Set<String> visitedUrls = new HashSet<>();

        // Initialize with seed URL at depth 0
        urlQueue.offer(new UrlDepthPair(seedUrl, 0));
        visitedUrls.add(normalizeUrl(seedUrl));

        while (!urlQueue.isEmpty() && result.getTotalPagesCrawled() < maxPages) {
            UrlDepthPair current = urlQueue.poll();
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

                // Create Page object and update result immutably
                Page page = new Page(url, title, 200, content, links);
                result = result.withSuccessfulPage(page);

                // Add new links to queue if within depth limit
                if (depth < maxDepth) {
                    // Functional approach: filter and collect new links to visit
                    links.stream()
                        .filter(link -> shouldFollowLink(link))
                        .filter(link -> !visitedUrls.contains(normalizeUrl(link)))
                        .forEach(link -> {
                            visitedUrls.add(normalizeUrl(link));
                            urlQueue.offer(new UrlDepthPair(link, depth + 1));
                        });
                }

            } catch (IOException e) {
                result = result.withFailedUrl(url);
            }
        }

        result = result.markComplete();
        return result;
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
     * Builder for creating SequentialCrawler instances with custom configuration.
     * Follows immutable builder pattern with functional validation.
     */
    public static class Builder {
        private int maxDepth = 2;
        private int maxPages = 50;
        private int timeoutMs = 5000;
        private boolean followExternalLinks = false;
        private String startDomain = "";

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
         * Builds a new SequentialCrawler with the configured settings.
         *
         * @return a new SequentialCrawler instance
         */
        public SequentialCrawler build() {
            return new SequentialCrawler(this);
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

