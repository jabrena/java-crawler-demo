package info.jab.crawler.v1;

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
}

