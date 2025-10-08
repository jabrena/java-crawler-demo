package info.jab.crawler.v5;

import info.jab.crawler.commons.Page;
import info.jab.crawler.v5.Message.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Worker actor that processes individual URL crawl requests.
 *
 * This actor is responsible for:
 * - Fetching web pages using Jsoup
 * - Extracting page content and links
 * - Sending results back to the supervisor
 * - Handling errors gracefully
 *
 * The actor processes URLs asynchronously using CompletableFuture
 * and communicates with the supervisor via message passing.
 */
public class WorkerActor {

    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final ExecutorService executor;
    private final Consumer<Message> messageSender;

    /**
     * Creates a new worker actor.
     *
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     * @param executor executor service for async processing
     * @param messageSender function to send messages back to supervisor
     */
    public WorkerActor(int timeoutMs, boolean followExternalLinks, String startDomain,
                      ExecutorService executor, Consumer<Message> messageSender) {
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
        this.executor = executor;
        this.messageSender = messageSender;
    }

    /**
     * Processes a URL crawl request asynchronously.
     *
     * @param crawlMessage the crawl request containing URL and depth
     */
    public void processUrl(CrawlMessage crawlMessage) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return fetchAndParsePage(crawlMessage.url());
            } catch (Exception e) {
                return new ErrorMessage(crawlMessage.url(), e);
            }
        }, executor)
        .thenAccept(result -> {
            if (result instanceof ErrorMessage errorMsg) {
                messageSender.accept(errorMsg);
            } else {
                // Extract links and create result message
                List<String> newLinks = extractLinks((Document) result);
                Page page = createPage(crawlMessage.url(), (Document) result);
                messageSender.accept(new ResultMessage(page, newLinks, crawlMessage.depth()));
            }
            // Always send completion message after processing
            messageSender.accept(new CompletionMessage());
        })
        .exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Fetches and parses a web page.
     *
     * @param url the URL to fetch
     * @return the parsed document
     * @throws IOException if the page cannot be fetched
     */
    private Document fetchAndParsePage(String url) throws IOException {
        return Jsoup.connect(url)
            .timeout(timeoutMs)
            .userAgent("Mozilla/5.0 (Educational Crawler)")
            .maxBodySize(1024 * 1024) // Limit body size to 1MB for performance
            .get();
    }

    /**
     * Creates a Page object from a parsed document.
     *
     * @param url the original URL
     * @param doc the parsed document
     * @return a Page object with extracted content
     */
    private Page createPage(String url, Document doc) {
        String title = doc.title();
        String content = doc.body().text();
        List<String> links = extractLinks(doc);

        return new Page(url, title, 200, content, links);
    }

    /**
     * Extracts all absolute links from a document.
     * Returns an immutable list following functional programming principles.
     *
     * @param doc the document to extract links from
     * @return list of absolute URLs
     */
    private List<String> extractLinks(Document doc) {
        return doc.select("a[href]")
            .stream()
            .map(element -> element.absUrl("href"))
            .filter(link -> !link.isEmpty())
            .filter(link -> link.startsWith("http://") || link.startsWith("https://"))
            .filter(this::shouldFollowLink)
            .limit(20) // Limit links per page for performance
            .toList();
    }

    /**
     * Determines if a link should be followed based on crawler configuration.
     *
     * @param url the URL to check
     * @return true if the link should be followed
     */
    private boolean shouldFollowLink(String url) {
        if (followExternalLinks) {
            return true;
        }
        // Only follow links from the same domain
        return url.contains(startDomain);
    }

    /**
     * Normalizes a URL by removing fragments and trailing slashes.
     * This is a pure function with no side effects.
     *
     * @param url the URL to normalize
     * @return normalized URL
     */
    private String normalizeUrl(String url) {
        // Handle null or empty URLs
        if (url == null || url.isEmpty()) {
            return url;
        }

        // More efficient normalization
        int hashIndex = url.indexOf('#');
        if (hashIndex != -1) {
            url = url.substring(0, hashIndex);
        }
        if (url.endsWith("/") && url.length() > 1) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
