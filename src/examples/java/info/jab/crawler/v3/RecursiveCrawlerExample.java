package info.jab.crawler.v3;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;

/**
 * Example demonstrating how to use the RecursiveCrawler with trampoline pattern.
 *
 * This example crawls the cursor-rules-java website and demonstrates:
 * - Creating a crawler using the external DefaultCrawlerBuilder
 * - Using CrawlerType enum to select the recursive implementation
 * - Starting a crawl from a seed URL using trampoline pattern
 * - Processing the crawl results
 * - Accessing page information
 * - Safe deep recursion without stack overflow
 *
 * To run this example:
 *   mvn compile exec:java -Dexec.mainClass="info.jab.crawler.v3.RecursiveCrawlerExample"
 */
public class RecursiveCrawlerExample {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("RecursiveCrawler with Trampoline Pattern - Usage Example");
        System.out.println("=".repeat(80));
        System.out.println();

        // Step 1: Configure and build the recursive crawler
        System.out.println("Step 1: Configuring the recursive crawler...");
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)    // Use recursive crawler with trampoline
            .maxDepth(2)                          // Crawl up to 2 levels deep (same as other examples)
            .maxPages(100)                         // Limit to 100 pages maximum
            .timeout(10000)                       // 10 second timeout per page
            .followExternalLinks(false)           // Stay within the same domain
            .startDomain("jabrena.github.io")     // Only follow links from this domain
            .build();

        System.out.println("✓ Recursive crawler configured successfully");
        System.out.println("✓ Using trampoline pattern for safe deep recursion");
        System.out.println();

        // Step 2: Start crawling from the seed URL
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("Step 2: Starting recursive crawl from: " + seedUrl);
        System.out.println("  - Using breadth-first traversal (same as other crawlers)");
        System.out.println("  - Trampoline prevents stack overflow");
        System.out.println();

        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(seedUrl);
        long endTime = System.currentTimeMillis();

        // Step 3: Process and display the results
        System.out.println("Step 3: Processing results...");
        System.out.println();

        displayCrawlStatistics(result, endTime - startTime);
        displaySuccessfulPages(result);
        displayFailedUrls(result);
    }

    /**
     * Displays crawl statistics and performance metrics.
     */
    private static void displayCrawlStatistics(CrawlResult result, long executionTimeMs) {
        System.out.println("📊 CRAWL STATISTICS");
        System.out.println("-".repeat(80));
        System.out.printf("Total Pages Crawled: %d%n", result.getTotalPagesCrawled());
        System.out.printf("Failed URLs:         %d%n", result.getTotalFailures());
        System.out.printf("Execution Time:      %d ms%n", executionTimeMs);
        System.out.printf("Average per Page:    %.2f ms%n",
            result.getTotalPagesCrawled() > 0 ? (double) executionTimeMs / result.getTotalPagesCrawled() : 0.0);
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
                System.out.printf("    Content:      %d characters%n", page.content().length());
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
}
