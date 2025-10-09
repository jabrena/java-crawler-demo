package info.jab.crawler.v10;

import info.jab.crawler.commons.Page;

import java.util.List;

/**
 * Message types for actor-based communication in the V10 crawler.
 *
 * This sealed interface defines all possible message types that can be passed
 * between actors in the virtual thread actor model implementation. Each message type is
 * immutable and represents a specific action or result in the crawling process.
 */
public sealed interface Message {

    /**
     * Message requesting a URL to be crawled.
     * Contains the URL and its depth in the crawl tree.
     */
    record CrawlMessage(String url, int depth) implements Message {
        public CrawlMessage {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
            if (depth < 0) {
                throw new IllegalArgumentException("Depth must be non-negative");
            }
        }
    }

    /**
     * Message containing the result of a successful page crawl.
     * Contains the crawled page, any new links discovered, and the depth.
     */
    record ResultMessage(Page page, List<String> newLinks, int depth) implements Message {
        public ResultMessage {
            if (page == null) {
                throw new IllegalArgumentException("Page cannot be null");
            }
            if (depth < 0) {
                throw new IllegalArgumentException("Depth must be non-negative");
            }
            // Defensive copy to ensure immutability
            newLinks = List.copyOf(newLinks);
        }
    }

    /**
     * Message indicating an error occurred while crawling a URL.
     * Contains the failed URL and the exception that occurred.
     */
    record ErrorMessage(String url, Exception error) implements Message {
        public ErrorMessage {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
            if (error == null) {
                throw new IllegalArgumentException("Error cannot be null");
            }
        }
    }

    /**
     * Message indicating that a worker actor has completed its work.
     * Used for coordination and completion detection.
     */
    record CompletionMessage() implements Message {}
}
