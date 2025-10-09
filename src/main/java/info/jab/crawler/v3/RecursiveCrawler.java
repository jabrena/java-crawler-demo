package info.jab.crawler.v3;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.Trampoline;
import info.jab.crawler.commons.UrlDepthPair;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * A recursive web crawler using the trampoline pattern for safe deep recursion.
 *
 * Design characteristics:
 * - Recursive approach using trampoline pattern to avoid stack overflow
 * - Breadth-first traversal of web pages (same as SequentialCrawler for consistent results)
 * - Maintains a visited set to avoid duplicates
 * - Respects maximum depth and page limits
 * - Uses Queue-based approach with functional recursion
 * - Uses continuation-passing style for recursion
 */
public class RecursiveCrawler implements Crawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;

    public RecursiveCrawler(int maxDepth, int maxPages, int timeoutMs, boolean followExternalLinks, String startDomain) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
    }

    /**
     * Crawls the web starting from the given seed URL using recursive approach with trampoline.
     *
     * @param seedUrl the starting URL for the crawl
     * @return CrawlResult containing all crawled pages and statistics
     */
    @Override
    public CrawlResult crawl(String seedUrl) {
        // Initialize state for recursive crawling
        Queue<UrlDepthPair> urlQueue = new ArrayDeque<>();
        Set<String> visitedUrls = new HashSet<>();

        // Initialize with seed URL at depth 0
        urlQueue.offer(new UrlDepthPair(seedUrl, 0));
        visitedUrls.add(Page.normalizeUrl(seedUrl));

        CrawlState initialState = new CrawlState(
            CrawlResult.empty(),
            urlQueue,
            visitedUrls
        );

        // Start recursive crawling with trampoline
        Trampoline<CrawlState> trampoline = crawlRecursively(initialState);

        // Execute the trampoline to get final result
        CrawlState finalState = trampoline.run();
        CrawlResult result = finalState.result.markComplete();

        return result;
    }

    /**
     * Recursively crawls URLs from the queue using trampoline pattern.
     *
     * @param state current crawl state
     * @return trampoline for safe recursion
     */
    private Trampoline<CrawlState> crawlRecursively(CrawlState state) {
        // Check termination conditions
        if (state.urlQueue.isEmpty() || state.result.getTotalPagesCrawled() >= maxPages) {
            return Trampoline.done(state);
        }

        // Get next URL to process
        UrlDepthPair current = state.urlQueue.poll();
        String url = current.url();
        int depth = current.depth();

        // Try to fetch the page
        try {
            // Fetch and parse the page using Page.fromUrl
            Page page = Page.fromUrl(url, timeoutMs);
            CrawlResult newResult = state.result.withSuccessfulPage(page);

            // Add new links to queue if within depth limit
            if (depth < maxDepth) {
                page.links().stream()
                    .filter(this::shouldFollowLink)
                    .filter(link -> !state.visitedUrls.contains(Page.normalizeUrl(link)))
                    .forEach(link -> {
                        state.visitedUrls.add(Page.normalizeUrl(link));
                        state.urlQueue.offer(new UrlDepthPair(link, depth + 1));
                    });
            }

            // Create new state with updated result
            CrawlState newState = new CrawlState(
                newResult,
                state.urlQueue,
                state.visitedUrls
            );

            // Continue with next URL using trampoline
            return Trampoline.more(() -> crawlRecursively(newState));

        } catch (IOException e) {
            // Handle fetch failure
            CrawlResult newResult = state.result.withFailedUrl(url);
            CrawlState newState = new CrawlState(
                newResult,
                state.urlQueue,
                state.visitedUrls
            );

            // Continue with next URL using trampoline
            return Trampoline.more(() -> crawlRecursively(newState));
        }
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
     * Immutable state object for tracking crawl progress.
     * This allows for pure functional recursion without mutable state.
     */
    private record CrawlState(
        CrawlResult result,
        Queue<UrlDepthPair> urlQueue,
        Set<String> visitedUrls
    ) {}
}
