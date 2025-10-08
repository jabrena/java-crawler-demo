package info.jab.crawler.v7;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;

/**
 * Example demonstrating the Structural Concurrency Web Crawler (V7).
 *
 * This crawler showcases Java 25's structural concurrency features with:
 * - StructuredTaskScope for managing concurrent subtasks within well-defined scopes
 * - Automatic cancellation and cleanup when scope closes
 * - Simplified error handling and propagation
 * - Natural tree-like crawling structure with proper resource management
 * - Virtual threads for efficient concurrency
 * - Structured scoping for concurrent operations
 *
 * Key features:
 * - StructuredTaskScope provides automatic resource management
 * - Virtual threads enable efficient concurrency without thread pool overhead
 * - Fault isolation between concurrent branches
 * - Natural tree-like crawling structure matching web topology
 * - Automatic cleanup when scope closes
 * - Simplified error handling and propagation
 * - Stack-safe deep recursion with structured scoping
 */
public class StructuralConcurrencyCrawlerExample {

    public static void main(String[] args) {
        System.out.println("🏗️ Structural Concurrency Web Crawler (V7) Example");
        System.out.println("==================================================");
        System.out.println();

        // Create the structural concurrency-based crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.STRUCTURAL_CONCURRENCY)    // Use V7 structural concurrency crawler
            .maxDepth(2)                                      // Crawl up to 2 levels deep
            .maxPages(100)                                    // Limit to 100 pages maximum
            .timeout(10000)                                   // 10 second timeout per page
            .followExternalLinks(false)                       // Stay within the same domain
            .startDomain("jabrena.github.io")                 // Only follow links from this domain
            .build();

        System.out.println("🔧 Crawler Configuration:");
        System.out.println("  - Type: Structural Concurrency (V7)");
        System.out.println("  - Max Depth: 2 levels");
        System.out.println("  - Max Pages: 100");
        System.out.println("  - Timeout: 10 seconds per page");
        System.out.println("  - Domain: jabrena.github.io only");
        System.out.println("  - Using Java 25's StructuredTaskScope for concurrent operations");
        System.out.println("  - Virtual threads for efficient concurrency");
        System.out.println("  - Automatic resource cleanup when scope closes");
        System.out.println("  - Fault isolation between concurrent branches");
        System.out.println("  - Natural tree-like crawling structure");
        System.out.println();

        // Start crawling
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("🌐 Starting structural concurrency crawl from: " + seedUrl);
        System.out.println();

        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(seedUrl);
        long endTime = System.currentTimeMillis();

        // Display results
        displayResults(result, endTime - startTime);
    }

    private static void displayResults(CrawlResult result, long executionTime) {
        System.out.println("📊 CRAWL STATISTICS");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Total Pages Crawled: %d%n", result.getTotalPagesCrawled());
        System.out.printf("Failed URLs:         %d%n", result.getTotalFailures());
        System.out.printf("Execution Time:      %d ms%n", executionTime);
        System.out.printf("Average per Page:    %.2f ms%n",
            result.getTotalPagesCrawled() > 0 ? (double) executionTime / result.getTotalPagesCrawled() : 0.0);
        System.out.printf("Throughput:          %.2f pages/sec%n",
            result.getTotalPagesCrawled() / (executionTime / 1000.0));
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
}
