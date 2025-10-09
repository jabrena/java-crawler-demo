package info.jab.crawler.v12;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.commons.DefaultCrawlerBuilder;

/**
 * Example demonstrating the Jox-based Structured Concurrency Crawler (V12).
 *
 * This crawler uses SoftwareMill's Jox library for supervised scopes, addressing
 * the SoftwareMill critique of JEP 505 by providing:
 *
 * 1. **Supervised Scopes**: Scope body runs in separate virtual thread with automatic supervision
 * 2. **Cancellable Forks**: Individual task cancellation support with `forkCancellable()`
 * 3. **Built-in Timeout**: Uses Jox's timeout mechanisms for page fetching
 * 4. **Clear Semantics**: `Fork.join()` provides clear blocking behavior (unlike Subtask.get())
 * 5. **Uniform Cancellation**: Scope body participates in error handling like subtasks
 *
 * Key advantages over JEP 505 StructuredTaskScope:
 * - Supervisor pattern with separate virtual thread for scope body
 * - Better error handling and automatic cleanup
 * - Individual task cancellation support
 * - Clear fork semantics without confusion with Future.get()
 * - Addresses all major critiques from the SoftwareMill article
 *
 * This demonstrates how to use Jox's supervised scopes for structured concurrency
 * while maintaining the benefits of automatic resource management and cancellation.
 */
public class JoxCrawlerExample {

    public static void main(String[] args) {
        System.out.println("🕷️  Jox-based Structured Concurrency Web Crawler (V12) Example");
        System.out.println("=================================================================");
        System.out.println();

        // Create the Jox-based structured concurrency crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.JOX_STRUCTURED_CONCURRENCY)  // Use V12 crawler
            .maxDepth(2)                                          // Crawl up to 2 levels deep
            .maxPages(50)                                         // Limit to 50 pages maximum
            .timeout(10000)                                       // 10 second timeout per page
            .followExternalLinks(false)                           // Stay within the same domain
            .startDomain("jabrena.github.io")                     // Only follow links from this domain
            .build();

        System.out.println("🔧 Crawler Configuration:");
        System.out.println("  - Type: Jox-based Structured Concurrency (V12)");
        System.out.println("  - Max Depth: 2 levels");
        System.out.println("  - Max Pages: 50");
        System.out.println("  - Timeout: 10 seconds per page");
        System.out.println("  - Domain: jabrena.github.io only");
        System.out.println("  - Using supervised() scopes for automatic supervision");
        System.out.println("  - Using forkCancellable() for individual task cancellation");
        System.out.println("  - Clear fork semantics with Fork.join()");
        System.out.println("  - Supervisor pattern with separate virtual thread");
        System.out.println();

        // Demonstrate Jox supervised scope features
        demonstrateJoxFeatures();

        // Start crawling
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("🌐 Starting crawl from: " + seedUrl);
        System.out.println();

        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(seedUrl);
        long endTime = System.currentTimeMillis();

        // Display results
        displayResults(result, endTime - startTime);
        displayJoxAdvantages();
    }

    /**
     * Demonstrates the key Jox features that address the SoftwareMill critique.
     */
    private static void demonstrateJoxFeatures() {
        System.out.println("🚀 JOX SUPERVISED SCOPE FEATURES");
        System.out.println("----------------------------------");

        System.out.println("✅ Supervised Scopes:");
        System.out.println("  - Scope body runs in separate virtual thread");
        System.out.println("  - Automatic supervision and cleanup");
        System.out.println("  - Better error handling than JEP 505");

        System.out.println("\n✅ Cancellable Forks:");
        System.out.println("  - Individual task cancellation support");
        System.out.println("  - Better control than standard forks");
        System.out.println("  - Clean cancellation propagation");

        System.out.println("\n✅ Clear Fork Semantics:");
        System.out.println("  - Fork.join() provides clear blocking behavior");
        System.out.println("  - No confusion with Future.get()");
        System.out.println("  - Natural blocking semantics");

        System.out.println("\n✅ Automatic Interruption:");
        System.out.println("  - Supervisor handles scope body interruption");
        System.out.println("  - Uniform cancellation across all tasks");
        System.out.println("  - Scope body participates in error handling");

        System.out.println("\n✅ User vs Daemon Forks:");
        System.out.println("  - forkUser() for required tasks");
        System.out.println("  - fork() for background tasks");
        System.out.println("  - Better control than JEP 505");

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

    private static void displayJoxAdvantages() {
        System.out.println("🚀 V12 JOX ADVANTAGES OVER JEP 505:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("✅ Supervised Scopes: Scope body runs in separate virtual thread with automatic supervision");
        System.out.println("✅ Cancellable Forks: Individual task cancellation support with forkCancellable()");
        System.out.println("✅ Clear Semantics: Fork.join() provides clear blocking behavior (unlike Subtask.get())");
        System.out.println("✅ Automatic Interruption: Supervisor handles scope body interruption");
        System.out.println("✅ Better Error Handling: Automatic cleanup and better error propagation");
        System.out.println("✅ User vs Daemon Forks: Control which forks must complete");
        System.out.println();

        System.out.println("📚 ADDRESSES SOFTWAREMILL CRITIQUE:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("🔧 Critique #1: Non-uniform cancellation");
        System.out.println("   Solution: Scope body now participates in error handling mechanisms");
        System.out.println();
        System.out.println("🔧 Critique #2: Scope logic split between Joiner and scope body");
        System.out.println("   Solution: Supervised scopes provide unified scope logic");
        System.out.println();
        System.out.println("🔧 Critique #3: Redundant timeout configuration parameter");
        System.out.println("   Solution: Jox provides built-in timeout mechanisms");
        System.out.println();
        System.out.println("🔧 Critique #4: Confusing method naming (Subtask.get() vs Future.get())");
        System.out.println("   Solution: Fork.join() provides clear completion semantics");
        System.out.println();

        System.out.println("💡 This V12 implementation demonstrates how to use Jox's supervised scopes");
        System.out.println("   to address the limitations identified in the SoftwareMill article while");
        System.out.println("   providing better control, clearer semantics, and automatic resource management.");
        System.out.println();
        System.out.println("🔗 Reference: https://softwaremill.com/critique-of-jep-505-structured-concurrency-fifth-preview/");
        System.out.println("🔗 Jox Library: https://github.com/softwaremill/jox");
    }
}
