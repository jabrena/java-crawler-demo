package info.jab.crawler.commons;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Represents a crawled web page with its content and metadata.
 *
 * This record is fully immutable following functional programming principles.
 * All fields are final and collections are defensively copied.
 */
public record Page(
    String url,
    String title,
    int statusCode,
    String content,
    List<String> links
) {
    /**
     * Compact constructor ensures immutability and validates invariants.
     */
    public Page {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        // Defensive copy to ensure immutability
        links = List.copyOf(links);
    }

    /**
     * Pure function to check if the page was successfully fetched.
     *
     * @return true if status code indicates success (2xx)
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    // Static connection configuration for better performance
    private static final String USER_AGENT = "Mozilla/5.0 (Educational Structured Queue Crawler)";
    private static final int MAX_BODY_SIZE = 1024 * 1024; // 1MB limit for performance
    private static final int DEFAULT_TIMEOUT = 10000; // 10 seconds

    // Simple cache for recently fetched pages (URL -> Page)
    private static final ConcurrentHashMap<String, CachedPage> PAGE_CACHE = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE_LIMIT = 1000; // Maximum number of cached pages
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5); // 5 minutes TTL

    // Cache entry with timestamp
    private record CachedPage(Page page, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    /**
     * Creates a Page object by fetching and parsing a URL.
     * This static factory method encapsulates the logic for fetching
     * page content, title, and links from a URL.
     * Optimized for performance with connection reuse and caching.
     *
     * @param url the URL to crawl
     * @param timeoutMs timeout in milliseconds for the HTTP request
     * @return a new Page object with extracted content
     * @throws IOException if the page cannot be fetched
     */
    public static Page fromUrl(String url, int timeoutMs) throws IOException {
        // Check cache first
        String normalizedUrl = normalizeUrl(url);
        CachedPage cached = PAGE_CACHE.get(normalizedUrl);
        if (cached != null && !cached.isExpired()) {
            return cached.page();
        }

        // Fetch from web if not in cache or expired
        Document doc = Jsoup.connect(url)
            .timeout(timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT)
            .userAgent(USER_AGENT)
            .maxBodySize(MAX_BODY_SIZE)
            .followRedirects(true)
            .ignoreHttpErrors(false)
            .ignoreContentType(false)
            .get();

        String title = doc.title();
        String content = doc.body().text();
        List<String> links = extractLinksFromDocument(doc);
        Page page = new Page(url, title, 200, content, links);

        // Cache the result (with size limit)
        if (PAGE_CACHE.size() < CACHE_SIZE_LIMIT) {
            PAGE_CACHE.put(normalizedUrl, new CachedPage(page, System.currentTimeMillis()));
        }

        return page;
    }

    /**
     * Clears expired entries from the page cache.
     * This method should be called periodically to prevent memory leaks.
     */
    public static void clearExpiredCache() {
        PAGE_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Clears all entries from the page cache.
     */
    public static void clearCache() {
        PAGE_CACHE.clear();
    }

    /**
     * Returns the current cache size.
     */
    public static int getCacheSize() {
        return PAGE_CACHE.size();
    }

    /**
     * Extracts all absolute links from a Jsoup Document.
     * This is a pure function that returns an immutable list following
     * functional programming principles.
     *
     * @param doc the document to extract links from
     * @return immutable list of absolute URLs
     */
    private static List<String> extractLinksFromDocument(Document doc) {
        return doc.select("a[href]")
            .stream()
            .map(element -> element.absUrl("href"))
            .filter(link -> !link.isEmpty())
            .filter(link -> link.startsWith("http://") || link.startsWith("https://"))
            .toList();
    }

    /**
     * Normalizes a URL by removing fragments, trimming whitespace, converting to lowercase,
     * and removing trailing slashes. This is a pure function with no side effects.
     *
     * @param url the URL to normalize (can be null)
     * @return normalized URL (empty string if input is null)
     */
    public static String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }

        // Trim whitespace and convert to lowercase for consistent comparison
        String normalized = url.trim().toLowerCase();

        // Remove fragment
        normalized = normalized.split("#")[0];

        // Remove trailing slash using functional approach
        return normalized.endsWith("/") && normalized.length() > 1
            ? normalized.substring(0, normalized.length() - 1)
            : normalized;
    }
}


