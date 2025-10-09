package info.jab.crawler.v11;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.commons.DefaultCrawlerBuilder;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Example demonstrating the Improved Structured Concurrency Crawler (V11).
 *
 * This crawler addresses the SoftwareMill critique of JEP 505 Structured Concurrency by implementing:
 *
 * 1. **Uniform Cancellation**: Scope body participates in error handling like subtasks
 * 2. **Unified Scope Logic**: No split between Joiner implementation and scope body
 * 3. **Timeout as Method**: Lightweight timeout pattern without configuration parameter
 * 4. **Custom Joiner**: Race semantics for better control over completion logic
 *
 * Key improvements over standard StructuredTaskScope:
 * - Custom UnifiedCancellationJoiner provides race semantics
 * - TimeoutUtil implements timeout-as-method pattern
 * - Scope body can signal completion to cancel remaining work
 * - All tasks participate in error handling uniformly
 * - Natural tree-like crawling structure with proper resource management
 *
 * This demonstrates how to overcome the limitations identified in the SoftwareMill article
 * while maintaining the benefits of structured concurrency.
 */
public class ImprovedStructuredCrawlerExample {

    public static void main(String[] args) {
        System.out.println("🕷️  Improved Structured Concurrency Web Crawler (V11) Example");
        System.out.println("=============================================================");
        System.out.println();

        // Create the improved structured concurrency crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)  // Use V11 crawler
            .maxDepth(2)                                               // Crawl up to 2 levels deep
            .maxPages(50)                                              // Limit to 50 pages maximum
            .timeout(10000)                                            // 10 second timeout per page
            .followExternalLinks(false)                                // Stay within the same domain
            .startDomain("jabrena.github.io")                          // Only follow links from this domain
            .build();

        System.out.println("🔧 Crawler Configuration:");
        System.out.println("  - Type: Improved Structured Concurrency (V11)");
        System.out.println("  - Max Depth: 2 levels");
        System.out.println("  - Max Pages: 50");
        System.out.println("  - Timeout: 10 seconds per page");
        System.out.println("  - Domain: jabrena.github.io only");
        System.out.println("  - Using UnifiedCancellationJoiner for race semantics");
        System.out.println("  - Using TimeoutUtil for timeout-as-method pattern");
        System.out.println("  - Uniform cancellation and unified scope logic");
        System.out.println();

        // Demonstrate timeout-as-method pattern
        demonstrateTimeoutAsMethod();

        // Start crawling
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("🌐 Starting crawl from: " + seedUrl);
        System.out.println();

        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(seedUrl);
        long endTime = System.currentTimeMillis();

        // Display results
        displayResults(result, endTime - startTime);
        displayImprovements();
    }

    /**
     * Demonstrates the timeout-as-method pattern from the SoftwareMill article.
     */
    private static void demonstrateTimeoutAsMethod() {
        System.out.println("⏱️  TIMEOUT-AS-METHOD DEMONSTRATION");
        System.out.println("-----------------------------------");

        try {
            // Demonstrate successful completion before timeout
            System.out.println("Testing quick task (should complete successfully):");
            String result = TimeoutUtil.timeout(Duration.ofMillis(1000), () -> {
                Thread.sleep(100); // Simulate quick work
                return "Quick task completed!";
            });
            System.out.println("✅ Result: " + result);

        } catch (Exception e) {
            System.out.println("❌ Unexpected error: " + e.getMessage());
        }

        try {
            // Demonstrate timeout behavior
            System.out.println("\nTesting slow task (should timeout):");
            String result = TimeoutUtil.timeout(Duration.ofMillis(100), () -> {
                Thread.sleep(500); // Simulate slow work that exceeds timeout
                return "Slow task completed!";
            });
            System.out.println("✅ Result: " + result);

        } catch (TimeoutException e) {
            System.out.println("⏰ Expected timeout: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Unexpected error: " + e.getMessage());
        }

        System.out.println("\n💡 Key Benefits of Timeout-as-Method:");
        System.out.println("  - No special configuration parameter needed");
        System.out.println("  - Lightweight timeout functionality");
        System.out.println("  - Demonstrates general pattern for resiliency");
        System.out.println("  - Uses StructuredTaskScope with race semantics");
        System.out.println();
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

    private static void displayImprovements() {
        System.out.println("🚀 V11 IMPROVEMENTS OVER STANDARD STRUCTURED CONCURRENCY:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("✅ Uniform Cancellation: Scope body participates in error handling like subtasks");
        System.out.println("✅ Unified Scope Logic: No split between Joiner implementation and scope body");
        System.out.println("✅ Timeout as Method: Lightweight timeout pattern without configuration parameter");
        System.out.println("✅ Custom Joiner: Race semantics for better control over completion logic");
        System.out.println("✅ Better Error Handling: Unified error handling across all tasks");
        System.out.println("✅ Improved Control: Scope body can signal completion to cancel remaining work");
        System.out.println();

        System.out.println("📚 ADDRESSES SOFTWAREMILL CRITIQUE:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("🔧 Critique #1: Non-uniform cancellation");
        System.out.println("   Solution: Scope body now participates in error handling mechanisms");
        System.out.println();
        System.out.println("🔧 Critique #2: Scope logic split between Joiner and scope body");
        System.out.println("   Solution: UnifiedCancellationJoiner provides unified scope logic");
        System.out.println();
        System.out.println("🔧 Critique #3: Redundant timeout configuration parameter");
        System.out.println("   Solution: TimeoutUtil implements timeout-as-method pattern");
        System.out.println();
        System.out.println("🔧 Critique #4: Confusing method naming (Subtask.get() vs Future.get())");
        System.out.println("   Solution: Custom Joiner provides clear completion semantics");
        System.out.println();

        System.out.println("💡 This V11 implementation demonstrates how to overcome the limitations");
        System.out.println("   identified in the SoftwareMill article while maintaining the benefits");
        System.out.println("   of structured concurrency and providing better control over completion logic.");
        System.out.println();
        System.out.println("🔗 Reference: https://softwaremill.com/critique-of-jep-505-structured-concurrency-fifth-preview/");
    }
}
