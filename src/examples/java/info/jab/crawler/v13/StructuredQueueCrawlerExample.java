package info.jab.crawler.v13;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;

/**
 * Example demonstrating how to use the StructuredQueueCrawler to crawl a website.
 *
 * This example crawls the cursor-rules-java website and demonstrates:
 * - Creating a structured concurrency crawler using the external DefaultCrawlerBuilder
 * - Using CrawlerType enum to select the structured queue implementation
 * - Configuring thread count for parallel processing with virtual threads
 * - Starting a crawl from a seed URL
 * - Processing the crawl results
 * - Accessing page information
 * - Benefits of structured concurrency over traditional thread pools
 *
 * To run this example:
 *   mvn compile exec:java -Dexec.mainClass="info.jab.crawler.v13.StructuredQueueCrawlerExample"
 */
public class StructuredQueueCrawlerExample {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("StructuredQueueCrawler (BlockingQueue + StructuredTaskScope) Usage Example");
        System.out.println("=".repeat(80));
        System.out.println();

        // Step 1: Configure and build the crawler
        System.out.println("Step 1: Configuring the structured queue crawler...");
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.STRUCTURED_QUEUE_CRAWLER) // Use structured queue crawler
            .maxDepth(2)                        // Crawl up to 2 levels deep
            .maxPages(100)                       // Limit to 100 pages maximum
            .timeout(10000)                     // 10 second timeout per page
            .numThreads(4)                      // Use 4 worker virtual threads
            .followExternalLinks(false)         // Stay within the same domain
            .startDomain("jabrena.github.io")   // Only follow links from this domain
            .build();

        System.out.println("✓ Crawler configured successfully (4 worker virtual threads)");
        System.out.println("✓ Uses StructuredTaskScope for automatic resource management");
        System.out.println("✓ No poison pill pattern needed - automatic cleanup");
        System.out.println();

        // Step 2: Start crawling from the seed URL
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("Step 2: Starting structured queue crawl from: " + seedUrl);

        CrawlResult result = crawler.crawl(seedUrl);

        // Step 3: Process and display the results
        System.out.println("Step 3: Processing results...");
        System.out.println();

        displayCrawlSummary(result);
        displaySuccessfulPages(result);
        displayFailedUrls(result);
        displayStructuredConcurrencyBenefits();
    }

    /**
     * Displays a summary of the crawl results.
     */
    private static void displayCrawlSummary(CrawlResult result) {
        System.out.println("📊 CRAWL SUMMARY");
        System.out.println("-".repeat(80));
        System.out.printf("  Total Pages Crawled:  %d%n", result.getTotalPagesCrawled());
        System.out.printf("  Failed URLs:          %d%n", result.getTotalFailures());
        System.out.printf("  Duration:             %dms%n", result.getDurationMs());
        System.out.printf("  Throughput:           %.2f pages/sec%n",
            result.getTotalPagesCrawled() / (result.getDurationMs() / 1000.0));
        System.out.println();
    }

    /**
     * Displays information about successfully crawled pages.
     */
    private static void displaySuccessfulPages(CrawlResult result) {
        System.out.println("✅ SUCCESSFULLY CRAWLED PAGES");
        System.out.println("-".repeat(80));

        if (result.successfulPages().isEmpty()) {
            System.out.println("No pages were successfully crawled.");
        } else {
            int pageNum = 1;
            for (Page page : result.successfulPages()) {
                System.out.printf("%n[%d] %s%n", pageNum++, page.url());
                System.out.printf("    Title:        %s%n",
                    page.title().isBlank() ? "(no title)" : page.title());
                System.out.printf("    Links Found:  %d%n", page.links().size());
            }
        }
        System.out.println();
    }

    /**
     * Displays failed URLs if any.
     */
    private static void displayFailedUrls(CrawlResult result) {
        if (result.failedUrls().isEmpty()) {
            return;
        }

        System.out.println("❌ FAILED URLS");
        System.out.println("-".repeat(80));
        for (String failedUrl : result.failedUrls()) {
            System.out.printf("  • %s%n", failedUrl);
        }
        System.out.println();
    }

    /**
     * Displays the benefits of structured concurrency.
     */
    private static void displayStructuredConcurrencyBenefits() {
        System.out.println("🚀 STRUCTURED CONCURRENCY BENEFITS");
        System.out.println("-".repeat(80));
        System.out.println("  ✓ Automatic resource cleanup via try-with-resources");
        System.out.println("  ✓ No ExecutorService shutdown complexity");
        System.out.println("  ✓ No poison pill pattern needed");
        System.out.println("  ✓ Virtual threads for efficient concurrency");
        System.out.println("  ✓ Better exception handling through scope hierarchy");
        System.out.println("  ✓ Proven BlockingQueue coordination from V2");
        System.out.println("  ✓ Simplified worker management");
        System.out.println();
        System.out.println("This implementation combines the best of:");
        System.out.println("  • V2's proven producer-consumer pattern");
        System.out.println("  • Java 25's structured concurrency features");
        System.out.println("  • Virtual threads for optimal I/O performance");
        System.out.println();
    }
}
