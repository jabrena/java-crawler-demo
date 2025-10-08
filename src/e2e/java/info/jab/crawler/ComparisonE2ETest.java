package info.jab.crawler;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End comparison test that verifies all crawler implementations
 * return consistent results when given the same configuration.
 *
 * This test crawls actual websites and should only be run when explicitly enabled
 * via Maven profile or system property to avoid hitting real sites during regular builds.
 *
 * Run with: mvn test -Pe2e
 * Or: mvn test -Dtest.e2e=true
 */
@EnabledIfSystemProperty(named = "test.e2e", matches = "true")
class ComparisonE2ETest {

    private static final String TARGET_URL = "https://jabrena.github.io/cursor-rules-java/";
    private static final int MAX_DEPTH = 2;
    private static final int MAX_PAGES = 10;
    private static final int TIMEOUT_MS = 10000;
    private static final String START_DOMAIN = "jabrena.github.io";

    @Test
    @DisplayName("E2E: All crawlers should return the same number of pages")
    void testAllCrawlersReturnSamePageCount() {
        // Given - Create all four crawler types with identical configuration
        Crawler sequentialCrawler = createCrawler(CrawlerType.SEQUENTIAL);
        Crawler producerConsumerCrawler = createCrawler(CrawlerType.PRODUCER_CONSUMER);
        Crawler recursiveCrawler = createCrawler(CrawlerType.RECURSIVE);
        Crawler multiThreadedRecursiveCrawler = createCrawler(CrawlerType.MULTI_THREADED_RECURSIVE);

        // When - Crawl the same URL with all four crawlers
        System.out.println("\n=== Crawling with SequentialCrawler ===");
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        System.out.println(sequentialResult);

        System.out.println("\n=== Crawling with ProducerConsumerCrawler ===");
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        System.out.println(producerConsumerResult);

        System.out.println("\n=== Crawling with RecursiveCrawler ===");
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);
        System.out.println(recursiveResult);

        System.out.println("\n=== Crawling with MultiThreadedRecursiveCrawler ===");
        CrawlResult multiThreadedRecursiveResult = multiThreadedRecursiveCrawler.crawl(TARGET_URL);
        System.out.println(multiThreadedRecursiveResult);

        // Then - All crawlers should return the same number of pages
        System.out.println("\n=== Comparison Results ===");
        System.out.printf("Sequential:                %d pages, %d failures%n",
            sequentialResult.getTotalPagesCrawled(),
            sequentialResult.getTotalFailures());
        System.out.printf("ProducerConsumer:          %d pages, %d failures%n",
            producerConsumerResult.getTotalPagesCrawled(),
            producerConsumerResult.getTotalFailures());
        System.out.printf("Recursive:                 %d pages, %d failures%n",
            recursiveResult.getTotalPagesCrawled(),
            recursiveResult.getTotalFailures());
        System.out.printf("MultiThreadedRecursive:    %d pages, %d failures%n",
            multiThreadedRecursiveResult.getTotalPagesCrawled(),
            multiThreadedRecursiveResult.getTotalFailures());

        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential and ProducerConsumer should crawl same number of pages")
            .isEqualTo(producerConsumerResult.getTotalPagesCrawled());

        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential and Recursive should crawl same number of pages")
            .isEqualTo(recursiveResult.getTotalPagesCrawled());

        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential and MultiThreadedRecursive should crawl same number of pages")
            .isEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled());

        assertThat(producerConsumerResult.getTotalPagesCrawled())
            .as("ProducerConsumer and Recursive should crawl same number of pages")
            .isEqualTo(recursiveResult.getTotalPagesCrawled());

        assertThat(producerConsumerResult.getTotalPagesCrawled())
            .as("ProducerConsumer and MultiThreadedRecursive should crawl same number of pages")
            .isEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled());

        assertThat(recursiveResult.getTotalPagesCrawled())
            .as("Recursive and MultiThreadedRecursive should crawl same number of pages")
            .isEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled());

        // All should crawl at least one page
        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("All crawlers should crawl at least one page")
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("E2E: All crawlers should discover the same URLs")
    void testAllCrawlersDiscoverSameUrls() {
        // Given - Create all three crawler types with identical configuration
        Crawler sequentialCrawler = createCrawler(CrawlerType.SEQUENTIAL);
        Crawler producerConsumerCrawler = createCrawler(CrawlerType.PRODUCER_CONSUMER);
        Crawler recursiveCrawler = createCrawler(CrawlerType.RECURSIVE);

        // When - Crawl the same URL with all three crawlers
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);

        // Extract URLs from each result
        Set<String> sequentialUrls = sequentialResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        Set<String> producerConsumerUrls = producerConsumerResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        Set<String> recursiveUrls = recursiveResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        // Then - All crawlers should discover the same URLs
        System.out.println("\n=== URL Comparison ===");
        System.out.println("Sequential URLs: " + sequentialUrls.size());
        System.out.println("ProducerConsumer URLs: " + producerConsumerUrls.size());
        System.out.println("Recursive URLs: " + recursiveUrls.size());

        assertThat(sequentialUrls)
            .as("Sequential and ProducerConsumer should discover same URLs")
            .containsExactlyInAnyOrderElementsOf(producerConsumerUrls);

        assertThat(sequentialUrls)
            .as("Sequential and Recursive should discover same URLs")
            .containsExactlyInAnyOrderElementsOf(recursiveUrls);

        assertThat(producerConsumerUrls)
            .as("ProducerConsumer and Recursive should discover same URLs")
            .containsExactlyInAnyOrderElementsOf(recursiveUrls);
    }

    @Test
    @DisplayName("E2E: All crawlers should have similar failure rates")
    void testAllCrawlersSimilarFailureRates() {
        // Given - Create all three crawler types with identical configuration
        Crawler sequentialCrawler = createCrawler(CrawlerType.SEQUENTIAL);
        Crawler producerConsumerCrawler = createCrawler(CrawlerType.PRODUCER_CONSUMER);
        Crawler recursiveCrawler = createCrawler(CrawlerType.RECURSIVE);

        // When - Crawl the same URL with all three crawlers
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);

        // Then - All crawlers should have similar failure counts
        // (exact match is not guaranteed due to timing and network variability)
        System.out.println("\n=== Failure Comparison ===");
        System.out.println("Sequential failures: " + sequentialResult.getTotalFailures());
        System.out.println("ProducerConsumer failures: " + producerConsumerResult.getTotalFailures());
        System.out.println("Recursive failures: " + recursiveResult.getTotalFailures());

        // Failures should be within reasonable range (allowing for some variability)
        int maxFailures = Math.max(
            Math.max(sequentialResult.getTotalFailures(), producerConsumerResult.getTotalFailures()),
            recursiveResult.getTotalFailures()
        );

        int minFailures = Math.min(
            Math.min(sequentialResult.getTotalFailures(), producerConsumerResult.getTotalFailures()),
            recursiveResult.getTotalFailures()
        );

        // Allow up to 2 failures difference due to network timing
        assertThat(maxFailures - minFailures)
            .as("Failure counts should be within 2 of each other")
            .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("E2E: All crawlers should respect depth limit consistently")
    void testAllCrawlersRespectDepthLimit() {
        // Given - Create crawlers with depth 0 (only seed URL)
        Crawler sequentialCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.SEQUENTIAL)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        Crawler producerConsumerCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.PRODUCER_CONSUMER)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        Crawler recursiveCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        // When - Crawl with depth limit of 0
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);

        // Then - All should crawl exactly 1 page (the seed URL)
        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential should crawl exactly 1 page with depth 0")
            .isEqualTo(1);

        assertThat(producerConsumerResult.getTotalPagesCrawled())
            .as("ProducerConsumer should crawl exactly 1 page with depth 0")
            .isEqualTo(1);

        assertThat(recursiveResult.getTotalPagesCrawled())
            .as("Recursive should crawl exactly 1 page with depth 0")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("E2E: All crawlers should respect page limit consistently")
    void testAllCrawlersRespectPageLimit() {
        // Given - Create crawlers with page limit of 3
        int pageLimit = 3;

        Crawler sequentialCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.SEQUENTIAL)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        Crawler producerConsumerCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.PRODUCER_CONSUMER)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        Crawler recursiveCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        // When - Crawl with page limit
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);

        // Then - All should respect the page limit
        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential should respect page limit")
            .isLessThanOrEqualTo(pageLimit);

        assertThat(producerConsumerResult.getTotalPagesCrawled())
            .as("ProducerConsumer should respect page limit")
            .isLessThanOrEqualTo(pageLimit);

        assertThat(recursiveResult.getTotalPagesCrawled())
            .as("Recursive should respect page limit")
            .isLessThanOrEqualTo(pageLimit);

        // All should crawl the same number of pages
        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("All crawlers should crawl same number of pages")
            .isEqualTo(producerConsumerResult.getTotalPagesCrawled())
            .isEqualTo(recursiveResult.getTotalPagesCrawled());
    }

    @Test
    @DisplayName("E2E: All crawlers should complete in reasonable time")
    void testAllCrawlersPerformanceComparison() {
        // Given - Create all three crawler types
        Crawler sequentialCrawler = createCrawler(CrawlerType.SEQUENTIAL);
        Crawler producerConsumerCrawler = createCrawler(CrawlerType.PRODUCER_CONSUMER);
        Crawler recursiveCrawler = createCrawler(CrawlerType.RECURSIVE);
        Crawler multiThreadedRecursiveCrawler = createCrawler(CrawlerType.MULTI_THREADED_RECURSIVE);

        // When - Measure execution time for each crawler
        long sequentialTime = measureCrawlTime(sequentialCrawler, TARGET_URL);
        long producerConsumerTime = measureCrawlTime(producerConsumerCrawler, TARGET_URL);
        long recursiveTime = measureCrawlTime(recursiveCrawler, TARGET_URL);
        long multiThreadedRecursiveTime = measureCrawlTime(multiThreadedRecursiveCrawler, TARGET_URL);

        // Then - Display performance comparison
        System.out.println("\n=== Performance Comparison ===");
        System.out.printf("Sequential:                %d ms%n", sequentialTime);
        System.out.printf("ProducerConsumer:          %d ms%n", producerConsumerTime);
        System.out.printf("Recursive:                 %d ms%n", recursiveTime);
        System.out.printf("MultiThreadedRecursive:    %d ms%n", multiThreadedRecursiveTime);

        // All should complete within reasonable time (60 seconds)
        assertThat(sequentialTime)
            .as("Sequential should complete in reasonable time")
            .isLessThan(60000);

        assertThat(producerConsumerTime)
            .as("ProducerConsumer should complete in reasonable time")
            .isLessThan(60000);

        assertThat(recursiveTime)
            .as("Recursive should complete in reasonable time")
            .isLessThan(60000);

        assertThat(multiThreadedRecursiveTime)
            .as("MultiThreadedRecursive should complete in reasonable time")
            .isLessThan(60000);

        // Performance analysis
        System.out.printf("ProducerConsumer speedup: %.2fx%n", (double) sequentialTime / producerConsumerTime);
        System.out.printf("MultiThreadedRecursive speedup: %.2fx%n", (double) sequentialTime / multiThreadedRecursiveTime);
        System.out.printf("MultiThreadedRecursive vs Recursive: %.2fx%n", (double) recursiveTime / multiThreadedRecursiveTime);
    }

    /**
     * Helper method to create a crawler with standard configuration.
     */
    private Crawler createCrawler(CrawlerType type) {
        if (type == CrawlerType.PRODUCER_CONSUMER || type == CrawlerType.MULTI_THREADED_RECURSIVE) {
            return new DefaultCrawlerBuilder()
                .crawlerType(type)
                .maxDepth(MAX_DEPTH)
                .maxPages(MAX_PAGES)
                .timeout(TIMEOUT_MS)
                .followExternalLinks(false)
                .startDomain(START_DOMAIN)
                .numThreads(4)
                .build();
        }

        return new DefaultCrawlerBuilder()
            .crawlerType(type)
            .maxDepth(MAX_DEPTH)
            .maxPages(MAX_PAGES)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();
    }

    /**
     * Helper method to measure crawl execution time.
     */
    private long measureCrawlTime(Crawler crawler, String url) {
        long startTime = System.currentTimeMillis();
        crawler.crawl(url);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
}

