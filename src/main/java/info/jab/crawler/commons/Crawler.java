package info.jab.crawler.commons;

/**
 * Interface for web crawlers that traverse and extract information from web pages.
 *
 * <p>Implementations of this interface should handle:
 * <ul>
 *   <li>Fetching and parsing web pages</li>
 *   <li>Following links according to configured rules</li>
 *   <li>Managing visited URLs to avoid duplicates</li>
 *   <li>Respecting depth and page limits</li>
 *   <li>Handling errors gracefully</li>
 * </ul>
 *
 * <p>The interface follows functional programming principles by returning
 * immutable results and avoiding side effects beyond I/O operations.
 */
public interface Crawler {

    /**
     * Crawls the web starting from the given seed URL.
     *
     * <p>The crawler will:
     * <ul>
     *   <li>Start at the provided seed URL</li>
     *   <li>Extract and follow links according to configuration</li>
     *   <li>Collect information from each visited page</li>
     *   <li>Stop when limits are reached or no more pages to crawl</li>
     * </ul>
     *
     * @param seedUrl the starting URL for the crawl (must be a valid HTTP/HTTPS URL)
     * @return an immutable CrawlResult containing all crawled pages and statistics
     * @throws IllegalArgumentException if seedUrl is null or invalid
     */
    CrawlResult crawl(String seedUrl);
}


