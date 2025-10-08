package info.jab.crawler.v5;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;

/**
 * Example demonstrating the Actor Model Web Crawler (V5).
 *
 * This crawler showcases the Actor model pattern with:
 * - CompletableFuture-based actors for asynchronous processing
 * - Supervisor pattern for fault tolerance and coordination
 * - Message passing between actors for communication
 * - Parallel processing with configurable actor count
 * - Thread-safe shared state management
 *
 * Key features:
 * - Asynchronous message passing between supervisor and worker actors
 * - Supervisor coordinates worker actors and manages global state
 * - Worker actors process individual URLs independently
 * - Fault tolerance through supervisor pattern
 * - Scalable parallel processing with configurable actor count
 * - Breadth-first traversal with parallel execution
 */
public class ActorCrawlerExample {

    public static void main(String[] args) {
        System.out.println("🎭 Actor Model Web Crawler (V5) Example");
        System.out.println("=========================================");
        System.out.println();

        // Create the actor-based crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.ACTOR)                    // Use V5 actor model crawler
            .maxDepth(2)                                      // Crawl up to 2 levels deep
            .maxPages(100)                                    // Limit to 100 pages maximum
            .timeout(10000)                                   // 10 second timeout per page
            .followExternalLinks(false)                       // Stay within the same domain
            .startDomain("jabrena.github.io")                 // Only follow links from this domain
            .numThreads(4)                                    // Use 4 worker actors for parallel processing
            .build();

        System.out.println("🔧 Crawler Configuration:");
        System.out.println("  - Type: Actor Model (V5)");
        System.out.println("  - Max Depth: 2 levels");
        System.out.println("  - Max Pages: 100");
        System.out.println("  - Timeout: 10 seconds per page");
        System.out.println("  - Worker Actors: 4 parallel workers");
        System.out.println("  - Domain: jabrena.github.io only");
        System.out.println("  - Using breadth-first traversal with parallel execution");
        System.out.println("  - Message passing between supervisor and worker actors");
        System.out.println("  - Fault tolerance through supervisor pattern");
        System.out.println();

        // Start crawling
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("🌐 Starting actor-based crawl from: " + seedUrl);
        System.out.println();

        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(seedUrl);
        long endTime = System.currentTimeMillis();

        // Display results
        displayResults(result, endTime - startTime);
        displayActorModelBenefits();
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

    private static void displayActorModelBenefits() {
        System.out.println("🎭 V5 ACTOR MODEL BENEFITS:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("✅ Asynchronous Processing: CompletableFuture-based actors for non-blocking execution");
        System.out.println("✅ Message Passing: Clean communication between supervisor and worker actors");
        System.out.println("✅ Supervisor Pattern: Fault tolerance and coordination through supervisor actor");
        System.out.println("✅ Parallel Processing: Multiple worker actors process URLs simultaneously");
        System.out.println("✅ Scalability: Configurable actor count for different workloads");
        System.out.println("✅ Thread Safety: Concurrent collections and atomic operations for shared state");
        System.out.println("✅ Fault Tolerance: Individual actor failures don't stop the entire crawl");
        System.out.println("✅ Clean Architecture: Separation of concerns between coordination and processing");
        System.out.println();
        System.out.println("💡 This V5 implementation demonstrates:");
        System.out.println("   - Actor model pattern with message passing");
        System.out.println("   - Supervisor pattern for fault tolerance");
        System.out.println("   - Asynchronous processing with CompletableFuture");
        System.out.println("   - Parallel execution with configurable worker count");
        System.out.println("   - Thread-safe coordination and state management");
        System.out.println("   - Clean separation between coordination and processing logic");
        System.out.println();
        System.out.println("🚀 Performance Characteristics:");
        System.out.println("   - Parallel processing of multiple URLs simultaneously");
        System.out.println("   - Asynchronous message passing reduces blocking");
        System.out.println("   - Supervisor coordinates work distribution efficiently");
        System.out.println("   - Fault tolerance ensures robust crawling even with failures");
        System.out.println("   - Scalable design adapts to different workload requirements");
    }
}
