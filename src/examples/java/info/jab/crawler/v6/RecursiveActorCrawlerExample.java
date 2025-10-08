package info.jab.crawler.v6;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;

/**
 * Example demonstrating the Recursive Actor Model Web Crawler (V6).
 *
 * This crawler showcases a hybrid approach that combines:
 * - Actor model pattern for asynchronous processing and fault tolerance
 * - Recursive design for natural tree-like crawling structure
 * - Trampoline pattern for safe deep recursion
 * - Dynamic actor spawning based on discovered links
 * - Message passing for coordination between actors
 *
 * Key features:
 * - Each actor can recursively spawn child actors for discovered links
 * - Natural tree-like crawling structure matching web topology
 * - Asynchronous processing with CompletableFuture
 * - Fault isolation - actor failures don't affect other branches
 * - Dynamic resource management - actors created on-demand
 * - Stack-safe deep recursion using trampoline pattern
 * - Parallel execution with configurable actor count
 */
public class RecursiveActorCrawlerExample {

    public static void main(String[] args) {
        System.out.println("🌳 Recursive Actor Model Web Crawler (V6) Example");
        System.out.println("=================================================");
        System.out.println();

        // Create the recursive actor-based crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)        // Use V6 recursive actor model crawler
            .maxDepth(2)                                    // Crawl up to 2 levels deep
            .maxPages(100)                                   // Limit to 100 pages maximum
            .timeout(10000)                                  // 10 second timeout per page
            .followExternalLinks(false)                      // Stay within the same domain
            .startDomain("jabrena.github.io")                // Only follow links from this domain
            .numThreads(4)                                    // Use 4 max concurrent actors
            .build();

        System.out.println("🔧 Crawler Configuration:");
        System.out.println("  - Type: Recursive Actor Model (V6)");
        System.out.println("  - Max Depth: 2 levels");
        System.out.println("  - Max Pages: 100");
        System.out.println("  - Timeout: 10 seconds per page");
        System.out.println("  - Max Actors: 4 concurrent actors");
        System.out.println("  - Domain: jabrena.github.io only");
        System.out.println("  - Using recursive actor spawning for natural tree structure");
        System.out.println("  - Stack-safe deep recursion with trampoline pattern");
        System.out.println("  - Dynamic actor creation based on discovered links");
        System.out.println("  - Fault isolation between actor branches");
        System.out.println();

        // Start crawling
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("🌐 Starting recursive actor-based crawl from: " + seedUrl);
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
