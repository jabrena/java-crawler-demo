package info.jab.crawler.v10;

import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import info.jab.crawler.v10.Message.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Supervisor actor that coordinates the crawling process using virtual threads.
 *
 * This actor is responsible for:
 * - Managing a pool of worker actors
 * - Distributing URLs to available workers
 * - Tracking visited URLs to avoid duplicates
 * - Aggregating results from workers
 * - Detecting when crawling is complete
 * - Managing the overall crawl state
 *
 * The supervisor uses message passing to coordinate with worker actors
 * and maintains thread-safe shared state using concurrent collections.
 * Virtual threads provide lightweight concurrency without the overhead
 * of traditional thread pools.
 */
public class SupervisorActor {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final boolean followExternalLinks;
    private final String startDomain;
    private final int numActors;

    // Thread-safe collections for coordination
    private final BlockingQueue<CrawlMessage> urlQueue;
    private final BlockingQueue<Message> messageQueue;
    private final ConcurrentHashMap<String, Boolean> visitedUrls;
    private final List<Page> successfulPages;
    private final List<String> failedUrls;
    private final AtomicInteger pagesCrawled;
    private final AtomicInteger activeWorkers;
    private final AtomicInteger pendingMessages;

    // Virtual thread executors for lightweight concurrency
    private final ExecutorService executor;
    private final ExecutorService supervisorExecutor;
    private final List<WorkerActor> workers;
    private volatile boolean isShuttingDown;

    /**
     * Creates a new supervisor actor.
     *
     * @param maxDepth maximum crawl depth
     * @param maxPages maximum number of pages to crawl
     * @param timeoutMs connection timeout in milliseconds
     * @param followExternalLinks whether to follow external links
     * @param startDomain the starting domain for link filtering
     * @param numActors number of worker actors to create
     */
    public SupervisorActor(int maxDepth, int maxPages, int timeoutMs,
                          boolean followExternalLinks, String startDomain, int numActors) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
        this.followExternalLinks = followExternalLinks;
        this.startDomain = startDomain;
        this.numActors = numActors;

        // Initialize thread-safe collections
        this.urlQueue = new LinkedBlockingQueue<>();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.visitedUrls = new ConcurrentHashMap<>();
        this.successfulPages = Collections.synchronizedList(new ArrayList<>());
        this.failedUrls = Collections.synchronizedList(new ArrayList<>());
        this.pagesCrawled = new AtomicInteger(0);
        this.activeWorkers = new AtomicInteger(0);
        this.pendingMessages = new AtomicInteger(0);

        // Create virtual thread executors for lightweight concurrency
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.supervisorExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.workers = new ArrayList<>();
        this.isShuttingDown = false;

        // Create worker actors
        for (int i = 0; i < numActors; i++) {
            WorkerActor worker = new WorkerActor(timeoutMs, followExternalLinks, startDomain,
                                               executor, message -> {
                                                   messageQueue.offer(message);
                                                   pendingMessages.incrementAndGet();
                                               });
            workers.add(worker);
        }
    }

    /**
     * Starts the crawling process with the given seed URL.
     *
     * @param seedUrl the starting URL for the crawl
     * @return a CompletableFuture that completes with the final CrawlResult
     */
    public CompletableFuture<CrawlResult> start(String seedUrl) {
        long startTime = System.currentTimeMillis();

        // Initialize with seed URL
        if (seedUrl != null && !seedUrl.isEmpty()) {
            String normalizedUrl = Page.normalizeUrl(seedUrl);
            if (visitedUrls.putIfAbsent(normalizedUrl, true) == null) {
                urlQueue.offer(new CrawlMessage(seedUrl, 0));
                pendingMessages.incrementAndGet();
            }
        }

        // Start the coordination loop using virtual threads
        return CompletableFuture.supplyAsync(() -> {
            try {
                return coordinateCrawling(startTime);
            } finally {
                // Don't call shutdown here - let the caller handle it
                // to avoid deadlock with the executor
            }
        }, supervisorExecutor);
    }

    /**
     * Main coordination loop that manages the crawling process.
     *
     * @param startTime the start time of the crawl
     * @return the final CrawlResult
     */
    private CrawlResult coordinateCrawling(long startTime) {
        int loopCount = 0;
        int maxLoops = 10000; // Prevent infinite loops

        while (!isShuttingDown && shouldContinueCrawling() && loopCount < maxLoops) {
            try {
                loopCount++;

                // Process pending messages
                processPendingMessages();

                // Distribute work to available workers
                distributeWork();

                // Check for completion
                if (isCrawlingComplete()) {
                    break;
                }

                // Small delay to prevent busy waiting
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        return new CrawlResult(successfulPages, failedUrls, startTime, endTime);
    }

    /**
     * Handles messages received from worker actors.
     *
     * @param message the message from a worker
     */
    private void handleWorkerMessage(Message message) {
        pendingMessages.decrementAndGet();

        if (message instanceof ResultMessage) {
            handleResultMessage((ResultMessage) message);
        } else if (message instanceof ErrorMessage) {
            handleErrorMessage((ErrorMessage) message);
        } else if (message instanceof CompletionMessage) {
            handleCompletionMessage();
        } else {
            // Unknown message type - log and ignore
            System.err.println("Unknown message type: " + message.getClass().getSimpleName());
        }
    }

    /**
     * Handles a successful result message from a worker.
     *
     * @param resultMsg the result message
     */
    private void handleResultMessage(ResultMessage resultMsg) {
        // Check if we've reached the page limit before processing
        if (pagesCrawled.get() >= maxPages) {
            return; // Skip processing if we've already reached the limit
        }

        // Add the page to successful results
        successfulPages.add(resultMsg.page());

        // Increment the page counter
        pagesCrawled.incrementAndGet();

        // Check if we've reached the page limit after incrementing
        if (pagesCrawled.get() >= maxPages) {
            isShuttingDown = true;
            return;
        }

        // Add new links to the queue if within depth limit
        if (resultMsg.page().url() != null && resultMsg.depth() < maxDepth) {
            resultMsg.newLinks().stream()
                .filter(this::shouldFollowLink)
                .filter(link -> {
                    String normalized = Page.normalizeUrl(link);
                    return visitedUrls.putIfAbsent(normalized, true) == null;
                })
                .forEach(link -> {
                    urlQueue.offer(new CrawlMessage(link, resultMsg.depth() + 1));
                    pendingMessages.incrementAndGet();
                });
        }
    }

    /**
     * Handles an error message from a worker.
     *
     * @param errorMsg the error message
     */
    private void handleErrorMessage(ErrorMessage errorMsg) {
        failedUrls.add(errorMsg.url());
    }

    /**
     * Handles a completion message from a worker.
     */
    private void handleCompletionMessage() {
        activeWorkers.decrementAndGet();
    }

    /**
     * Processes any pending messages in the queue.
     */
    private void processPendingMessages() {
        Message message;
        while ((message = messageQueue.poll()) != null) {
            handleWorkerMessage(message);
        }
    }

    /**
     * Distributes work to available workers.
     */
    private void distributeWork() {
        while (!urlQueue.isEmpty() && activeWorkers.get() < numActors && !isShuttingDown) {
            CrawlMessage message = urlQueue.poll();
            if (message != null) {
                // Find an available worker and assign the task
                int workerIndex = activeWorkers.get() % numActors;
                WorkerActor worker = workers.get(workerIndex);
                activeWorkers.incrementAndGet();
                worker.processUrl(message);
            }
        }
    }

    /**
     * Determines if crawling should continue.
     *
     * @return true if crawling should continue
     */
    private boolean shouldContinueCrawling() {
        return pagesCrawled.get() < maxPages &&
               !isShuttingDown &&
               (activeWorkers.get() > 0 || !urlQueue.isEmpty() || !messageQueue.isEmpty());
    }

    /**
     * Determines if crawling is complete.
     *
     * @return true if crawling is complete
     */
    private boolean isCrawlingComplete() {
        return pagesCrawled.get() >= maxPages ||
               (activeWorkers.get() == 0 && urlQueue.isEmpty() && messageQueue.isEmpty());
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
     * Shuts down the supervisor and all worker actors.
     */
    public void shutdown() {
        isShuttingDown = true;

        // Shutdown virtual thread executors
        supervisorExecutor.shutdown();
        executor.shutdown();

        try {
            // Wait for supervisor executor to finish (with shorter timeout)
            if (!supervisorExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                supervisorExecutor.shutdownNow();
            }

            // Wait for worker executor to finish (with shorter timeout)
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            supervisorExecutor.shutdownNow();
            executor.shutdownNow();
        }
    }
}
