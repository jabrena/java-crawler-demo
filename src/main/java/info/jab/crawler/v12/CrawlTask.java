package info.jab.crawler.v12;

/**
 * Represents a crawl task with URL and depth information for Jox-based structured concurrency.
 *
 * This record encapsulates the essential information needed for crawling a single URL
 * within the context of a supervised scope. It provides validation and utility methods
 * for managing crawl depth and creating child tasks.
 *
 * @param url the URL to crawl
 * @param depth the current crawl depth
 */
public record CrawlTask(String url, int depth) {

    /**
     * Creates a new crawl task with the given URL and depth.
     *
     * @param url the URL to crawl (must not be null or empty)
     * @param depth the current crawl depth (must not be negative)
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
     * Checks if this task is at or beyond the maximum allowed depth.
     *
     * @param maxDepth the maximum allowed depth
     * @return true if this task's depth is at or beyond the maximum
     */
    public boolean isAtMaxDepth(int maxDepth) {
        return depth >= maxDepth;
    }

    /**
     * Checks if this task is within the allowed depth range.
     *
     * @param maxDepth the maximum allowed depth
     * @return true if this task's depth is within the allowed range
     */
    public boolean isWithinDepthLimit(int maxDepth) {
        return depth < maxDepth;
    }
}
