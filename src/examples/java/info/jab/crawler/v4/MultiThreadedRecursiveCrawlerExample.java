package info.jab.crawler.v4;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.commons.DefaultCrawlerBuilder;

/**
 * Example demonstrating the Multi-threaded Recursive Crawler (V4).
 * 
 * This crawler combines the best of both worlds:
 * - Multi-threading for parallel performance (from ProducerConsumerCrawler)
 * - Recursive design with trampoline pattern for stack safety (from RecursiveCrawler)
 * - Thread-safe coordination between recursive tasks
 * 
 * Key features:
 * - Parallel processing of multiple URLs simultaneously
 * - Stack-safe deep recursion using trampoline pattern
 * - Thread-safe shared state management
 * - Breadth-first traversal with parallel execution
 * - Optimal performance for large-scale crawling
 */
public class MultiThreadedRecursiveCrawlerExample {
    
    public static void main(String[] args) {
        System.out.println("🕷️  Multi-threaded Recursive Web Crawler (V4) Example");
        System.out.println("=====================================================");
        System.out.println();
        
        // Create the multi-threaded recursive crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.MULTI_THREADED_RECURSIVE)  // Use V4 crawler
            .maxDepth(2)                                        // Crawl up to 2 levels deep
            .maxPages(100)                                      // Limit to 100 pages maximum
            .timeout(10000)                                     // 10 second timeout per page
            .followExternalLinks(false)                         // Stay within the same domain
            .startDomain("jabrena.github.io")                   // Only follow links from this domain
            .numThreads(4)                                      // Use 4 threads for parallel processing
            .build();
        
        System.out.println("🔧 Crawler Configuration:");
        System.out.println("  - Type: Multi-threaded Recursive (V4)");
        System.out.println("  - Max Depth: 2 levels");
        System.out.println("  - Max Pages: 100");
        System.out.println("  - Timeout: 10 seconds per page");
        System.out.println("  - Threads: 4 parallel workers");
        System.out.println("  - Domain: jabrena.github.io only");
        System.out.println("  - Using breadth-first traversal with parallel execution");
        System.out.println("  - Stack-safe recursion with trampoline pattern");
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
        System.out.println("🚀 V4 PERFORMANCE BENEFITS:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("✅ Multi-threading: Parallel processing of multiple URLs");
        System.out.println("✅ Stack Safety: Trampoline pattern prevents stack overflow");
        System.out.println("✅ Recursive Design: Elegant functional programming approach");
        System.out.println("✅ Thread Safety: Coordinated shared state management");
        System.out.println("✅ Optimal Performance: Best of both parallel and recursive approaches");
        System.out.println("✅ Scalability: Configurable thread pool size");
        System.out.println();
        System.out.println("💡 This V4 implementation combines:");
        System.out.println("   - ProducerConsumerCrawler's parallel performance");
        System.out.println("   - RecursiveCrawler's elegant recursive design");
        System.out.println("   - Stack-safe trampoline pattern for deep recursion");
        System.out.println("   - Thread-safe coordination for reliable results");
    }
}
