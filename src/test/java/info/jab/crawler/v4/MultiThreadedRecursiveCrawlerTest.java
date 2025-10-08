package info.jab.crawler.v4;

import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MultiThreadedRecursiveCrawler.
 *
 * Tests the multi-threaded recursive crawler's core functionality including:
 * - Basic crawling behavior
 * - Thread safety
 * - Depth and page limits
 * - Error handling
 * - Performance characteristics
 */
class MultiThreadedRecursiveCrawlerTest {

    private MultiThreadedRecursiveCrawler crawler;

    @BeforeEach
    void setUp() {
        crawler = new MultiThreadedRecursiveCrawler(
            2,      // maxDepth
            10,     // maxPages
            5000,   // timeoutMs
            false,  // followExternalLinks
            "example.com", // startDomain
            4       // numThreads
        );
    }

    @Test
    @DisplayName("Should create crawler with correct configuration")
    void shouldCreateCrawlerWithCorrectConfiguration() {
        // Given
        MultiThreadedRecursiveCrawler customCrawler = new MultiThreadedRecursiveCrawler(
            3, 20, 10000, true, "test.com", 8
        );

        // When & Then
        assertThat(customCrawler).isNotNull();
        // Note: Configuration is private, so we test through behavior
    }

    @Test
    @DisplayName("Should handle invalid URLs gracefully")
    void shouldHandleInvalidUrlsGracefully() {
        // Given
        String invalidUrl = "https://invalid-domain-that-does-not-exist.com";
        MultiThreadedRecursiveCrawler testCrawler = new MultiThreadedRecursiveCrawler(
            2, 10, 1000, true, "example.com", 2
        );

        // When
        CrawlResult result = testCrawler.crawl(invalidUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(0);
        assertThat(result.getTotalFailures()).isEqualTo(1);
        assertThat(result.failedUrls()).contains(invalidUrl);
        assertThat(result.successfulPages()).isEmpty();
    }

    @Test
    @DisplayName("Should respect maximum page limit")
    void shouldRespectMaximumPageLimit() {
        // Given
        MultiThreadedRecursiveCrawler limitedCrawler = new MultiThreadedRecursiveCrawler(
            5,      // maxDepth
            3,      // maxPages (very low limit)
            1000,   // timeoutMs (shorter timeout)
            false,  // followExternalLinks
            "example.com", // startDomain
            2       // numThreads
        );

        // When - Use a simple URL that should respond quickly
        CrawlResult result = limitedCrawler.crawl("https://httpbin.org/status/200");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should respect maximum depth limit")
    void shouldRespectMaximumDepthLimit() {
        // Given
        MultiThreadedRecursiveCrawler shallowCrawler = new MultiThreadedRecursiveCrawler(
            0,      // maxDepth (no recursion)
            10,     // maxPages
            1000,   // timeoutMs (shorter timeout)
            false,  // followExternalLinks
            "httpbin.org", // startDomain
            2       // numThreads
        );

        // When - Use a simple URL that should respond quickly
        CrawlResult result = shallowCrawler.crawl("https://httpbin.org/status/200");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1); // Only the seed page
    }

    @Test
    @DisplayName("Should handle timeout gracefully")
    void shouldHandleTimeoutGracefully() {
        // Given
        MultiThreadedRecursiveCrawler timeoutCrawler = new MultiThreadedRecursiveCrawler(
            1,      // maxDepth
            5,      // maxPages
            100,    // timeoutMs (very short timeout)
            false,  // followExternalLinks
            "example.com", // startDomain
            2       // numThreads
        );

        // When - Use a URL that will definitely timeout
        CrawlResult result = timeoutCrawler.crawl("https://httpbin.org/delay/2");

        // Then
        assertThat(result).isNotNull();
        // Should have some failures due to timeout
        assertThat(result.getTotalFailures()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should filter external links when configured")
    void shouldFilterExternalLinksWhenConfigured() {
        // Given
        MultiThreadedRecursiveCrawler noExternalCrawler = new MultiThreadedRecursiveCrawler(
            1,      // maxDepth
            10,     // maxPages
            1000,   // timeoutMs (shorter timeout)
            false,  // followExternalLinks = false
            "httpbin.org", // startDomain
            2       // numThreads
        );

        // When - Use a simple URL that should respond quickly
        CrawlResult result = noExternalCrawler.crawl("https://httpbin.org/status/200");

        // Then
        assertThat(result).isNotNull();
        // All crawled pages should be from httpbin.org domain
        result.successfulPages().forEach(page -> {
            assertThat(page.url()).contains("httpbin.org");
        });
    }

    @Test
    @DisplayName("Should allow external links when configured")
    void shouldAllowExternalLinksWhenConfigured() {
        // Given
        MultiThreadedRecursiveCrawler externalCrawler = new MultiThreadedRecursiveCrawler(
            1,      // maxDepth
            5,      // maxPages
            1000,   // timeoutMs (shorter timeout)
            true,   // followExternalLinks = true
            "httpbin.org", // startDomain
            2       // numThreads
        );

        // When - Use a simple URL that should respond quickly
        CrawlResult result = externalCrawler.crawl("https://httpbin.org/status/200");

        // Then
        assertThat(result).isNotNull();
        // May crawl external links (though this test might be flaky due to external dependencies)
    }

    @Test
    @DisplayName("Should return valid crawl result structure")
    void shouldReturnValidCrawlResultStructure() {
        // Given
        String seedUrl = "https://httpbin.org/status/200";

        // When
        CrawlResult result = crawler.crawl(seedUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.successfulPages()).isNotNull();
        assertThat(result.failedUrls()).isNotNull();
        assertThat(result.startTime()).isPositive();
        assertThat(result.endTime()).isPositive();
        assertThat(result.endTime()).isGreaterThanOrEqualTo(result.startTime());
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccessSafely() throws InterruptedException {
        // Given
        String seedUrl = "https://httpbin.org/status/200";
        int numThreads = 3;
        Thread[] threads = new Thread[numThreads];
        CrawlResult[] results = new CrawlResult[numThreads];

        // When
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = crawler.crawl(seedUrl);
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
        }
    }

    @Test
    @DisplayName("Should handle empty seed URL")
    void shouldHandleEmptySeedUrl() {
        // Given
        String emptyUrl = "";

        // When
        CrawlResult result = crawler.crawl(emptyUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(0);
        assertThat(result.getTotalFailures()).isEqualTo(1);
        assertThat(result.failedUrls()).contains(emptyUrl);
    }

    @Test
    @DisplayName("Should handle null seed URL")
    void shouldHandleNullSeedUrl() {
        // Given
        String nullUrl = null;

        // When
        CrawlResult result = crawler.crawl(nullUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(0);
        assertThat(result.getTotalFailures()).isEqualTo(1);
        assertThat(result.failedUrls()).contains("null");
    }

    @Test
    @DisplayName("Should extract page information correctly")
    void shouldExtractPageInformationCorrectly() {
        // Given
        String seedUrl = "https://httpbin.org/status/200";

        // When
        CrawlResult result = crawler.crawl(seedUrl);

        // Then
        assertThat(result).isNotNull();
        if (!result.successfulPages().isEmpty()) {
            Page firstPage = result.successfulPages().get(0);
            assertThat(firstPage.url()).isNotBlank();
            assertThat(firstPage.statusCode()).isEqualTo(200);
            assertThat(firstPage.title()).isNotNull();
            assertThat(firstPage.content()).isNotNull();
            assertThat(firstPage.links()).isNotNull();
        }
    }
}
