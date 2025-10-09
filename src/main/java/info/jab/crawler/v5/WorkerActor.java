package info.jab.crawler.v5;

import info.jab.crawler.commons.Page;
import info.jab.crawler.v5.Message.*;
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
                return Page.fromUrl(crawlMessage.url(), timeoutMs);
            } catch (Exception e) {
                return new ErrorMessage(crawlMessage.url(), e);
            }
        }, executor)
        .thenAccept(result -> {
            if (result instanceof ErrorMessage errorMsg) {
                messageSender.accept(errorMsg);
            } else {
                // Create result message using Page object
                Page page = (Page) result;
                messageSender.accept(new ResultMessage(page, page.links(), crawlMessage.depth()));
            }
            // Always send completion message after processing
            messageSender.accept(new CompletionMessage());
        })
        .exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }
}
