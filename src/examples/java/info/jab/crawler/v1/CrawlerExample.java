package info.jab.crawler.v1;

/**
 * Example demonstrating how to use the SequentialCrawler to crawl a website.
 *
 * This example crawls the cursor-rules-java website and demonstrates:
 * - Creating a crawler with custom configuration
 * - Starting a crawl from a seed URL
 * - Processing the crawl results
 * - Accessing page information
 *
 * To run this example:
 *   mvn compile exec:java -Dexec.mainClass="info.jab.crawler.v1.CrawlerUsageExample"
 */
public class CrawlerExample {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("SequentialCrawler Usage Example");
        System.out.println("=".repeat(80));
        System.out.println();

        // Step 1: Configure and build the crawler
        System.out.println("Step 1: Configuring the crawler...");
        Crawler crawler = new SequentialCrawler.Builder()
            .maxDepth(2)                        // Crawl up to 2 levels deep
            .maxPages(100)                       // Limit to 10 pages maximum
            .timeout(10000)                     // 10 second timeout per page
            .followExternalLinks(false)         // Stay within the same domain
            .startDomain("jabrena.github.io")   // Only follow links from this domain
            .build();

        System.out.println("✓ Crawler configured successfully");
        System.out.println();

        // Step 2: Start crawling from the seed URL
        String seedUrl = "https://jabrena.github.io/cursor-rules-java/";
        System.out.println("Step 2: Starting crawl from: " + seedUrl);

        CrawlResult result = crawler.crawl(seedUrl);

        // Step 3: Process and display the results
        System.out.println("Step 3: Processing results...");
        System.out.println();

        displaySuccessfulPages(result);
        displayFailedUrls(result);
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
}

