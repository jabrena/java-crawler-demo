package info.jab.crawler.commons;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

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

    /**
     * Creates a Page object by fetching and parsing a URL.
     * This static factory method encapsulates the logic for fetching
     * page content, title, and links from a URL.
     *
     * @param url the URL to crawl
     * @param timeoutMs timeout in milliseconds for the HTTP request
     * @return a new Page object with extracted content
     * @throws IOException if the page cannot be fetched
     */
    public static Page fromUrl(String url, int timeoutMs) throws IOException {
        Document doc = Jsoup.connect(url)
            .timeout(timeoutMs)
            .userAgent("Mozilla/5.0 (Educational Structured Queue Crawler)")
            .get();

        String title = doc.title();
        String content = doc.body().text();
        List<String> links = extractLinksFromDocument(doc);
        return new Page(url, title, 200, content, links);
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
     * Normalizes a URL by removing fragments and trailing slashes.
     * This is a pure function with no side effects.
     *
     * @param url the URL to normalize
     * @return normalized URL
     */
    public static String normalizeUrl(String url) {
        // Remove fragment
        String normalized = url.split("#")[0];
        // Remove trailing slash using functional approach
        return normalized.endsWith("/")
            ? normalized.substring(0, normalized.length() - 1)
            : normalized;
    }
}


