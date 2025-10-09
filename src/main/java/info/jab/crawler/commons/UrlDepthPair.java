package info.jab.crawler.commons;

/**
 * Represents a URL and its depth in the crawl tree.
 *
 * This record is used across multiple crawler implementations to track
 * URLs and their current depth during the crawling process.
 *
 * @param url the URL to crawl (can be null for poison pill pattern)
 * @param depth the current depth in the crawl tree (0 for seed URL, can be negative for poison pill)
 */
public record UrlDepthPair(String url, int depth) {

    /**
     * Creates a new UrlDepthPair with the given URL and depth.
     *
     * @param url the URL to crawl (can be null for poison pill pattern)
     * @param depth the current depth (can be negative for poison pill pattern)
     * @throws IllegalArgumentException if url is empty (but null is allowed) or depth is negative for non-null URLs
     */
    public UrlDepthPair {
        // Allow null URLs for poison pill pattern (used in v2 and v4 crawlers)
        if (url != null && url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty (null is allowed for poison pill pattern)");
        }
        // Allow negative depth for poison pill pattern
        if (url != null && depth < 0) {
            throw new IllegalArgumentException("Depth cannot be negative for valid URLs");
        }
    }

    /**
     * Creates a poison pill UrlDepthPair for signaling shutdown in multi-threaded crawlers.
     *
     * @return a UrlDepthPair with null URL and negative depth
     */
    public static UrlDepthPair poisonPill() {
        return new UrlDepthPair(null, -1);
    }

    /**
     * Checks if this UrlDepthPair is a poison pill.
     *
     * @return true if this is a poison pill (null URL and negative depth)
     */
    public boolean isPoisonPill() {
        return url == null && depth < 0;
    }
}
