package info.jab.crawler.v1;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the aggregated results from a crawling session.
 *
 * This record is truly immutable, following functional programming principles.
 * State changes produce new instances rather than modifying existing ones.
 */
public record CrawlResult(
    List<Page> successfulPages,
    List<String> failedUrls,
    long startTime,
    long endTime
) {

    /**
     * Compact constructor ensures immutability by defensively copying lists.
     */
    public CrawlResult {
        successfulPages = List.copyOf(successfulPages);
        failedUrls = List.copyOf(failedUrls);
    }

    /**
     * Creates a new empty CrawlResult with current timestamp.
     *
     * @return a new CrawlResult with empty collections
     */
    public static CrawlResult empty() {
        return new CrawlResult(List.of(), List.of(), System.currentTimeMillis(), 0L);
    }

    /**
     * Returns a new CrawlResult with an additional successful page.
     * This is a pure function that doesn't modify the original.
     *
     * @param page the page to add
     * @return a new CrawlResult with the page added
     */
    public CrawlResult withSuccessfulPage(Page page) {
        List<Page> updatedPages = new ArrayList<>(successfulPages);
        updatedPages.add(page);
        return new CrawlResult(updatedPages, failedUrls, startTime, endTime);
    }

    /**
     * Returns a new CrawlResult with an additional failed URL.
     * This is a pure function that doesn't modify the original.
     *
     * @param url the failed URL
     * @return a new CrawlResult with the failed URL added
     */
    public CrawlResult withFailedUrl(String url) {
        List<String> updatedFailures = new ArrayList<>(failedUrls);
        updatedFailures.add(url);
        return new CrawlResult(successfulPages, updatedFailures, startTime, endTime);
    }

    /**
     * Returns a new CrawlResult marked as complete with the current end time.
     * This is a pure function that doesn't modify the original.
     *
     * @return a new CrawlResult with the end time set
     */
    public CrawlResult markComplete() {
        return new CrawlResult(successfulPages, failedUrls, startTime, System.currentTimeMillis());
    }

    /**
     * Returns the total number of successfully crawled pages.
     *
     * @return count of successful pages
     */
    public int getTotalPagesCrawled() {
        return successfulPages.size();
    }

    /**
     * Returns the total number of failed URLs.
     *
     * @return count of failures
     */
    public int getTotalFailures() {
        return failedUrls.size();
    }

    /**
     * Returns the duration of the crawl in milliseconds.
     *
     * @return duration in milliseconds, or 0 if not yet complete
     */
    public long getDurationMs() {
        return endTime > 0 ? endTime - startTime : 0L;
    }

    /**
     * Returns whether the crawl has been marked as complete.
     *
     * @return true if complete, false otherwise
     */
    public boolean isComplete() {
        return endTime > 0;
    }

    @Override
    public String toString() {
        return String.format(
            "CrawlResult[successful=%d, failed=%d, duration=%dms]",
            getTotalPagesCrawled(),
            getTotalFailures(),
            getDurationMs()
        );
    }
}

