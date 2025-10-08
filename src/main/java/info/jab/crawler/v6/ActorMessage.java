package info.jab.crawler.v6;

import info.jab.crawler.commons.Page;

import java.util.List;

/**
 * Message types for recursive actor-based communication in the V6 crawler.
 *
 * This sealed interface defines all possible message types that can be passed
 * between recursive actors in the actor model implementation. Each message type is
 * immutable and represents a specific action or result in the recursive crawling process.
 */
public sealed interface ActorMessage {

    /**
     * Message requesting a URL to be crawled recursively.
     * Contains the URL and its depth in the crawl tree.
     */
    record CrawlRequestMessage(String url, int depth) implements ActorMessage {
        public CrawlRequestMessage {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
            if (depth < 0) {
                throw new IllegalArgumentException("Depth must be non-negative");
            }
        }
    }

    /**
     * Message containing the result of a successful recursive page crawl.
     * Contains the crawled page, any new links discovered, and the depth.
     */
    record CrawlResultMessage(Page page, List<String> newLinks, int depth) implements ActorMessage {
        public CrawlResultMessage {
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
     * Message indicating an error occurred while crawling a URL recursively.
     * Contains the failed URL and the exception that occurred.
     */
    record CrawlErrorMessage(String url, Exception error) implements ActorMessage {
        public CrawlErrorMessage {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
            if (error == null) {
                throw new IllegalArgumentException("Error cannot be null");
            }
        }
    }

    /**
     * Message indicating that a recursive actor has completed its work.
     * Used for coordination and completion detection.
     */
    record ActorCompletionMessage() implements ActorMessage {}

    /**
     * Message for spawning a new child actor.
     * Contains the URL and depth for the new actor.
     */
    record SpawnChildMessage(String url, int depth) implements ActorMessage {
        public SpawnChildMessage {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
            if (depth < 0) {
                throw new IllegalArgumentException("Depth must be non-negative");
            }
        }
    }
}
