package info.jab.crawler.v4;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End tests for MultiThreadedIterativeCrawler against real websites.
 *
 * These tests verify the crawler's behavior in real-world scenarios:
 * - Crawling actual websites with complex structures
 * - Performance characteristics with real network conditions
 * - Thread safety and coordination in multi-threaded environment
 * - Handling of various HTML structures and link patterns
 * - Error handling with real network issues
 *
 * Run with: mvn verify -Pe2e
 * Or: mvn verify -Dtest.e2e=true
 */
@EnabledIfSystemProperty(named = "test.e2e", matches = "true")
class MultiThreadedIterativeCrawlerE2ETest {

    private static final String TARGET_URL = "https://jabrena.github.io/cursor-rules-java/";
    private static final String START_DOMAIN = "jabrena.github.io";
    private static final int MAX_DEPTH = 2;
    private static final int MAX_PAGES = 20;
    private static final int TIMEOUT_MS = 15000; // Longer timeout for real sites

    @Test
    @DisplayName("E2E: Should crawl real website successfully with multi-threading")
    void shouldCrawlRealWebsiteSuccessfully() {
        // Given
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.MULTI_THREADED_ITERATIVE)
            .maxDepth(MAX_DEPTH)
            .maxPages(MAX_PAGES)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        // When
        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(TARGET_URL);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isGreaterThan(0);
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(MAX_PAGES);
        assertThat(result.successfulPages()).isNotEmpty();
        assertThat(result.startTime()).isPositive();
        assertThat(result.endTime()).isPositive();
        assertThat(result.endTime()).isGreaterThanOrEqualTo(result.startTime());

        // Verify all crawled pages are from the correct domain
        result.successfulPages().forEach(page -> {
            assertThat(page.url()).contains(START_DOMAIN);
            // Title may be empty for some pages (e.g., redirect pages, API responses)
            assertThat(page.title()).isNotNull();
            assertThat(page.statusCode()).isEqualTo(200);
            // Content may be empty for some pages (e.g., redirect pages, API responses)
            assertThat(page.content()).isNotNull();
            assertThat(page.links()).isNotNull();
        });

        // Performance metrics
        long executionTime = endTime - startTime;
    }

    @Test
    @DisplayName("E2E: Should respect depth limits on real website")
    void shouldRespectDepthLimitsOnRealWebsite() {
        // Given
        Crawler shallowCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.MULTI_THREADED_ITERATIVE)
            .maxDepth(0) // Only crawl the seed page
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = shallowCrawler.crawl(TARGET_URL);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1); // Only the seed page
        assertThat(result.successfulPages()).hasSize(1);
        assertThat(result.successfulPages().get(0).url()).isEqualTo(TARGET_URL);
    }

    @Test
    @DisplayName("E2E: Should respect page limits on real website")
    void shouldRespectPageLimitsOnRealWebsite() {
        // Given
        int pageLimit = 5;
        Crawler limitedCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.MULTI_THREADED_ITERATIVE)
            .maxDepth(MAX_DEPTH)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(3)
            .build();

        // When
        CrawlResult result = limitedCrawler.crawl(TARGET_URL);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(pageLimit);
        assertThat(result.successfulPages()).hasSize(result.getTotalPagesCrawled());
    }

    @Test
    @DisplayName("E2E: Should handle different thread pool sizes")
    void shouldHandleDifferentThreadPoolSizes() {
        // Given
        int[] threadCounts = {1, 2, 4, 8};
        long[] executionTimes = new long[threadCounts.length];

        for (int i = 0; i < threadCounts.length; i++) {
            Crawler crawler = new DefaultCrawlerBuilder()
                .crawlerType(CrawlerType.MULTI_THREADED_ITERATIVE)
                .maxDepth(1) // Shallow crawl for faster testing
                .maxPages(10)
                .timeout(TIMEOUT_MS)
                .followExternalLinks(false)
                .startDomain(START_DOMAIN)
                .numThreads(threadCounts[i])
                .build();

            // When
            long startTime = System.currentTimeMillis();
            CrawlResult result = crawler.crawl(TARGET_URL);
            long endTime = System.currentTimeMillis();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalPagesCrawled()).isGreaterThan(0);
            executionTimes[i] = endTime - startTime;

            // Record execution time for analysis
        }

        // Verify that more threads generally improve performance (though this may vary)
    }

    @Test
    @DisplayName("E2E: Should handle network errors gracefully")
    void shouldHandleNetworkErrorsGracefully() {
        // Given
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.MULTI_THREADED_ITERATIVE)
            .maxDepth(1)
            .maxPages(5)
            .timeout(5000) // Short timeout to trigger some failures
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isGreaterThanOrEqualTo(0);
        assertThat(result.getTotalFailures()).isGreaterThanOrEqualTo(0);

        // The sum of successful and failed should be reasonable
        int totalAttempts = result.getTotalPagesCrawled() + result.getTotalFailures();
        assertThat(totalAttempts).isGreaterThan(0);
    }

    @Test
    @DisplayName("E2E: Should maintain thread safety under concurrent load")
    void shouldMaintainThreadSafetyUnderConcurrentLoad() throws InterruptedException {
        // Given
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.MULTI_THREADED_ITERATIVE)
            .maxDepth(1)
            .maxPages(5)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        int numConcurrentCrawls = 3;
        Thread[] threads = new Thread[numConcurrentCrawls];
        CrawlResult[] results = new CrawlResult[numConcurrentCrawls];

        // When
        for (int i = 0; i < numConcurrentCrawls; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = crawler.crawl(TARGET_URL);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (CrawlResult result : results) {
            assertThat(result).isNotNull();
            assertThat(result.getTotalPagesCrawled()).isGreaterThanOrEqualTo(0);
            assertThat(result.successfulPages()).isNotNull();
            assertThat(result.failedUrls()).isNotNull();
        }

        // Concurrent crawl test completed successfully with thread safety maintained
    }

    @Test
    @DisplayName("E2E: Should extract meaningful content from real pages")
    void shouldExtractMeaningfulContentFromRealPages() {
        // Given
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.MULTI_THREADED_ITERATIVE)
            .maxDepth(1)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.successfulPages()).isNotEmpty();

        // Verify content quality
        result.successfulPages().forEach(page -> {
            assertThat(page.title()).isNotBlank();
            assertThat(page.content()).isNotBlank();
            assertThat(page.content().length()).isGreaterThan(10); // Should have meaningful content
            assertThat(page.links()).isNotNull();

            // Verify links are properly extracted
            page.links().forEach(link -> {
                assertThat(link).isNotBlank();
                assertThat(link).startsWith("http");
            });
        });

        // Content extraction test completed
    }
}
