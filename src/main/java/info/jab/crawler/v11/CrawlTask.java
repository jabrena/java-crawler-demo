package info.jab.crawler.v11;

/**
 * Represents a crawl task with URL and depth information.
 *
 * This immutable record is used to pass crawl tasks between structured scopes
 * and provides a clean way to encapsulate the URL and its current depth
 * in the crawl tree.
 *
 * @param url the URL to crawl
 * @param depth the current depth in the crawl tree (0 for seed URL)
 */
public record CrawlTask(String url, int depth) {

    /**
     * Creates a new crawl task with the given URL and depth.
     *
     * @param url the URL to crawl (must not be null or empty)
     * @param depth the current depth (must not be negative)
     * @throws IllegalArgumentException if url is null/empty or depth is negative
     */
    public CrawlTask {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("Depth cannot be negative");
        }
    }

    /**
     * Creates a new crawl task for the next depth level.
     *
     * @return a new CrawlTask with the same URL but depth incremented by 1
     */
    public CrawlTask nextDepth() {
        return new CrawlTask(url, depth + 1);
    }

    /**
     * Checks if this task is at the maximum allowed depth.
     *
     * @param maxDepth the maximum allowed depth
     * @return true if this task's depth is at or exceeds the maximum
     */
    public boolean isAtMaxDepth(int maxDepth) {
        return depth >= maxDepth;
    }
}
