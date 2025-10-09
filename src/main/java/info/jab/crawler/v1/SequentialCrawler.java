package info.jab.crawler.v1;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.UrlDepthPair;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
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

    public SequentialCrawler(int maxDepth, int maxPages, int timeoutMs, boolean followExternalLinks, String startDomain) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
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
        visitedUrls.add(Page.normalizeUrl(seedUrl));

        while (!urlQueue.isEmpty() && result.getTotalPagesCrawled() < maxPages) {
            UrlDepthPair current = urlQueue.poll();
            String url = current.url();
            int depth = current.depth();

            try {
                // Fetch and parse the page using centralized method
                Page page = Page.fromUrl(url, timeoutMs);

                // Update result immutably
                result = result.withSuccessfulPage(page);

                // Add new links to queue if within depth limit
                if (depth < maxDepth) {
                    // Functional approach: filter and collect new links to visit
                    page.links().stream()
                        .filter(link -> shouldFollowLink(link))
                        .filter(link -> !visitedUrls.contains(Page.normalizeUrl(link)))
                        .forEach(link -> {
                            visitedUrls.add(Page.normalizeUrl(link));
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
