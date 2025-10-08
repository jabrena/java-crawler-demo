package info.jab.crawler.v9;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.commons.DefaultCrawlerBuilder;

/**
 * Example demonstrating the Structured Worker Crawler (V9).
 *
 * This crawler combines the best of both worlds:
 * - BlockingQueue + worker pattern for proven coordination (from MultiThreadedIterativeCrawler)
 * - StructuredTaskScope for automatic resource management and cleanup
 * - Virtual threads for efficient concurrency without thread pool overhead
 *
 * Key features:
 * - Automatic resource cleanup when scope closes
 * - Better error propagation through structured scoping
 * - Virtual threads for efficient concurrency
 * - No ExecutorService shutdown complexity
 * - No poison pill pattern needed (use scope cancellation)
 * - Modern Java 25 structured concurrency primitives
 */
public class StructuredWorkerCrawlerExample {

    public static void main(String[] args) {
        System.out.println("🕷️  Structured Worker Web Crawler (V9) Example");
        System.out.println("===============================================");
        System.out.println();

        // Create the structured worker crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.STRUCTURED_WORKER)  // Use V9 crawler
            .maxDepth(2)                                 // Crawl up to 2 levels deep
            .maxPages(100)                               // Limit to 100 pages maximum
            .timeout(10000)                              // 10 second timeout per page
            .followExternalLinks(false)                  // Stay within the same domain
            .startDomain("jabrena.github.io")            // Only follow links from this domain
            .numThreads(4)                               // Use 4 virtual threads for parallel processing
            .build();

        System.out.println("🔧 Crawler Configuration:");
        System.out.println("  - Type: Structured Worker (V9)");
        System.out.println("  - Max Depth: 2 levels");
        System.out.println("  - Max Pages: 100");
        System.out.println("  - Timeout: 10 seconds per page");
        System.out.println("  - Threads: 4 virtual workers");
        System.out.println("  - Domain: jabrena.github.io only");
        System.out.println("  - Using BlockingQueue + StructuredTaskScope coordination");
        System.out.println("  - Automatic resource cleanup and error handling");
        System.out.println();

        // Start crawling
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("🌐 Starting crawl from: " + seedUrl);
        System.out.println();

        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(seedUrl);
        long endTime = System.currentTimeMillis();

        // Display results
        displayResults(result, endTime - startTime);
        displayPerformanceBenefits();
    }

    private static void displayResults(CrawlResult result, long executionTime) {
        System.out.println("📊 CRAWL STATISTICS");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Total Pages Crawled: %d%n", result.getTotalPagesCrawled());
        System.out.printf("Failed URLs:         %d%n", result.getTotalFailures());
        System.out.printf("Execution Time:      %d ms%n", executionTime);
        System.out.printf("Average per Page:    %.2f ms%n", (double) executionTime / result.getTotalPagesCrawled());
        System.out.println();

        if (!result.successfulPages().isEmpty()) {
            System.out.println("📄 SAMPLE PAGES:");
            System.out.println("--------------------------------------------------------------------------------");
            result.successfulPages().stream()
                .limit(5)
                .forEach(page -> {
                    System.out.printf("✅ %s%n", page.url());
                    System.out.printf("   Title: %s%n", page.title());
                    System.out.printf("   Links: %d%n", page.links().size());
                    System.out.println();
                });

            if (result.successfulPages().size() > 5) {
                System.out.printf("... and %d more pages%n", result.successfulPages().size() - 5);
                System.out.println();
            }
        }

        if (!result.failedUrls().isEmpty()) {
            System.out.println("❌ FAILED URLS:");
            System.out.println("--------------------------------------------------------------------------------");
            result.failedUrls().forEach(url -> System.out.printf("❌ %s%n", url));
            System.out.println();
        }
    }

    private static void displayPerformanceBenefits() {
        System.out.println("🚀 V9 PERFORMANCE BENEFITS:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("✅ Structured Concurrency: Automatic resource management and cleanup");
        System.out.println("✅ Virtual Threads: Efficient concurrency without thread pool overhead");
        System.out.println("✅ Better Error Handling: Structured error propagation through scoping");
        System.out.println("✅ No Shutdown Complexity: No ExecutorService shutdown needed");
        System.out.println("✅ No Poison Pills: Use scope cancellation instead");
        System.out.println("✅ Modern Java 25: Latest structured concurrency primitives");
        System.out.println();
        System.out.println("💡 This V9 implementation combines:");
        System.out.println("   - MultiThreadedIterativeCrawler's proven BlockingQueue + worker pattern");
        System.out.println("   - Java 25's StructuredTaskScope for automatic resource management");
        System.out.println("   - Virtual threads for efficient concurrency");
        System.out.println("   - Better error handling and propagation");
        System.out.println("   - Simplified coordination without complex shutdown logic");
    }
}
