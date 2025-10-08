package info.jab.crawler.v8;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;

/**
 * Example demonstrating the Hybrid Actor-Structural Concurrency Web Crawler (V8).
 *
 * This crawler showcases a hybrid approach combining:
 * - Actor model pattern for coordination and state management
 * - Structural concurrency for actual crawling work with automatic resource management
 * - Message passing for actor coordination and state updates
 * - StructuredTaskScope for efficient parallel crawling with virtual threads
 * - Supervisor pattern for fault tolerance and coordination
 *
 * Key features:
 * - SupervisorActor provides centralized state management and coordination
 * - StructuredTaskScope provides automatic resource management
 * - Virtual threads enable efficient concurrency without thread pool overhead
 * - Fault isolation between actor coordination and crawling tasks
 * - Natural tree-like crawling structure matching web topology
 * - Automatic cleanup when scope closes
 * - Message-based coordination with actor model benefits
 * - Stack-safe deep recursion with structured scoping
 */
public class HybridActorStructuralCrawlerExample {

    public static void main(String[] args) {
        System.out.println("🏗️ Hybrid Actor-Structural Concurrency Web Crawler (V8) Example");
        System.out.println("==============================================================");
        System.out.println();

        // Create the hybrid actor-structural concurrency-based crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)    // Use V8 hybrid crawler
            .maxDepth(2)                                        // Crawl up to 2 levels deep
            .maxPages(100)                                      // Limit to 100 pages maximum
            .timeout(10000)                                     // 10 second timeout per page
            .followExternalLinks(false)                         // Stay within the same domain
            .startDomain("jabrena.github.io")                   // Only follow links from this domain
            .numThreads(8)                                      // 8 concurrent tasks
            .build();

        System.out.println("🔧 Crawler Configuration:");
        System.out.println("  - Type: Hybrid Actor-Structural Concurrency (V8)");
        System.out.println("  - Max Depth: 2 levels");
        System.out.println("  - Max Pages: 100");
        System.out.println("  - Timeout: 10 seconds per page");
        System.out.println("  - Domain: jabrena.github.io only");
        System.out.println("  - Concurrent Tasks: 8");
        System.out.println("  - Using Java 25's StructuredTaskScope for concurrent operations");
        System.out.println("  - SupervisorActor for state management and coordination");
        System.out.println("  - Virtual threads for efficient concurrency");
        System.out.println("  - Automatic resource cleanup when scope closes");
        System.out.println("  - Fault isolation between actor coordination and crawling tasks");
        System.out.println("  - Natural tree-like crawling structure");
        System.out.println("  - Message-based coordination with actor model benefits");
        System.out.println();

        // Start crawling
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("🌐 Starting hybrid actor-structural concurrency crawl from: " + seedUrl);
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
