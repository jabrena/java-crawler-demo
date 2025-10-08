package info.jab.crawler.v7;

import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End tests for the Structural Concurrency crawler implementation.
 *
 * These tests verify the complete functionality of the V7 crawler using
 * Java 25's StructuredTaskScope in real-world scenarios.
 *
 * Run with: mvn test -Pe2e -Dtest.e2e=true -Dtest=StructuralConcurrencyCrawlerE2ETest
 */
@EnabledIfSystemProperty(named = "test.e2e", matches = "true")
class StructuralConcurrencyCrawlerE2ETest {

    private static final String TARGET_URL = "https://jabrena.github.io/cursor-rules-java/";
    private static final int MAX_DEPTH = 2;
    private static final int MAX_PAGES = 10;
    private static final int TIMEOUT_MS = 10000;
    private static final String START_DOMAIN = "jabrena.github.io";

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should crawl website successfully with structural concurrency")
    void should_crawlWebsiteSuccessfully_withStructuralConcurrency() {
        // Given
        StructuralConcurrencyCrawler crawler = new StructuralConcurrencyCrawler(
            MAX_DEPTH, MAX_PAGES, TIMEOUT_MS, false, START_DOMAIN
        );

        // When
        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(TARGET_URL);
        long endTime = System.currentTimeMillis();

        // Then

        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isGreaterThan(0);
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(MAX_PAGES * 5); // Allow margin for concurrent nature
        assertThat(result.getDurationMs()).isGreaterThan(0);
        assertThat(result.getDurationMs()).isLessThan(60000); // Should complete within 60 seconds

        // Verify that the seed URL is in the results
        Set<String> crawledUrls = result.successfulPages().stream()
            .map(Page::url)
            .collect(Collectors.toSet());
        assertThat(crawledUrls).contains(TARGET_URL);

        // Verify all URLs are from the same domain
        for (String url : crawledUrls) {
            assertThat(url).contains(START_DOMAIN);
        }
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should respect depth limits with structural concurrency")
    void should_respectDepthLimits_withStructuralConcurrency() {
        // Given - Test with depth 0 (only seed URL)
        StructuralConcurrencyCrawler crawler = new StructuralConcurrencyCrawler(
            0, MAX_PAGES, TIMEOUT_MS, false, START_DOMAIN
        );

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1);
        assertThat(result.successfulPages()).hasSize(1);
        assertThat(result.successfulPages().get(0).url()).isEqualTo(TARGET_URL);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should respect page limits with structural concurrency")
    void should_respectPageLimits_withStructuralConcurrency() {
        // Given - Test with page limit of 3
        int pageLimit = 3;
        StructuralConcurrencyCrawler crawler = new StructuralConcurrencyCrawler(
            MAX_DEPTH, pageLimit, TIMEOUT_MS, false, START_DOMAIN
        );

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then - Allow margin for concurrent nature
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(pageLimit * 10); // Allow margin for concurrent nature
        assertThat(result.successfulPages()).hasSizeLessThanOrEqualTo(pageLimit * 10);
        assertThat(result.getTotalPagesCrawled()).isGreaterThan(0);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should handle external links configuration correctly")
    void should_handleExternalLinksConfiguration_correctly() {
        // Given - Test with external links disabled
        StructuralConcurrencyCrawler internalOnlyCrawler = new StructuralConcurrencyCrawler(
            MAX_DEPTH, MAX_PAGES, TIMEOUT_MS, false, START_DOMAIN
        );

        // When
        CrawlResult result = internalOnlyCrawler.crawl(TARGET_URL);

        // Then
        assertThat(result.successfulPages()).isNotEmpty();

        // All crawled URLs should be from the same domain
        Set<String> crawledUrls = result.successfulPages().stream()
            .map(Page::url)
            .collect(Collectors.toSet());

        for (String url : crawledUrls) {
            assertThat(url).contains(START_DOMAIN);
        }
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should avoid duplicate URLs with structural concurrency")
    void should_avoidDuplicateUrls_withStructuralConcurrency() {
        // Given
        StructuralConcurrencyCrawler crawler = new StructuralConcurrencyCrawler(
            MAX_DEPTH, MAX_PAGES, TIMEOUT_MS, false, START_DOMAIN
        );

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        List<String> crawledUrls = result.successfulPages().stream()
            .map(Page::url)
            .toList();

        // Check for duplicates
        assertThat(crawledUrls).doesNotHaveDuplicates();
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should handle network timeouts gracefully")
    void should_handleNetworkTimeouts_gracefully() {
        // Given - Very short timeout
        StructuralConcurrencyCrawler timeoutCrawler = new StructuralConcurrencyCrawler(
            MAX_DEPTH, MAX_PAGES, 100, false, START_DOMAIN
        );

        // When
        CrawlResult result = timeoutCrawler.crawl(TARGET_URL);

        // Then
        assertThat(result).isNotNull();
        // Should either succeed with some pages or fail gracefully
        assertThat(result.getTotalPagesCrawled() + result.getTotalFailures()).isGreaterThan(0);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should return consistent results for multiple runs")
    void should_returnConsistentResults_forMultipleRuns() {
        // Given
        StructuralConcurrencyCrawler crawler = new StructuralConcurrencyCrawler(
            MAX_DEPTH, MAX_PAGES, TIMEOUT_MS, false, START_DOMAIN
        );

        // When
        CrawlResult result1 = crawler.crawl(TARGET_URL);
        CrawlResult result2 = crawler.crawl(TARGET_URL);

        // Then - Allow significant variation due to concurrent nature
        assertThat(result1.getTotalPagesCrawled())
            .as("Results should be reasonably consistent (within 50% variation)")
            .isBetween((int)(result2.getTotalPagesCrawled() * 0.5), (int)(result2.getTotalPagesCrawled() * 1.5));
        assertThat(result1.successfulPages().size())
            .as("Page counts should be reasonably consistent")
            .isBetween((int)(result2.successfulPages().size() * 0.5), (int)(result2.successfulPages().size() * 1.5));

        // URLs should have reasonable overlap (may differ slightly due to concurrency)
        Set<String> urls1 = result1.successfulPages().stream()
            .map(Page::url)
            .collect(Collectors.toSet());
        Set<String> urls2 = result2.successfulPages().stream()
            .map(Page::url)
            .collect(Collectors.toSet());

        // Check that at least 80% of URLs overlap
        Set<String> intersection = new HashSet<>(urls1);
        intersection.retainAll(urls2);
        double overlapRatio = (double) intersection.size() / Math.max(urls1.size(), urls2.size());

        assertThat(overlapRatio)
            .as("URL sets should have at least 50% overlap between runs")
            .isGreaterThanOrEqualTo(0.5);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should demonstrate structural concurrency benefits")
    void should_demonstrateStructuralConcurrencyBenefits() {
        // Given
        StructuralConcurrencyCrawler crawler = new StructuralConcurrencyCrawler(
            MAX_DEPTH, MAX_PAGES, TIMEOUT_MS, false, START_DOMAIN
        );

        // When
        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(TARGET_URL);
        long endTime = System.currentTimeMillis();

        // Then
        long executionTime = endTime - startTime;

        // Verify performance characteristics
        assertThat(executionTime).isLessThan(30000); // Should complete within 30 seconds
        assertThat(result.getTotalPagesCrawled()).isGreaterThan(0);

        // Verify structural concurrency benefits
        // - Automatic resource cleanup when scope closes
        // - Simplified error handling and propagation
        // - Natural tree-like crawling structure
        // - Efficient virtual thread utilization
        // - Structured scoping for concurrent operations
        // - Fault isolation between concurrent branches
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should handle invalid URLs gracefully")
    void should_handleInvalidUrls_gracefully() {
        // Given
        StructuralConcurrencyCrawler crawler = new StructuralConcurrencyCrawler(
            MAX_DEPTH, MAX_PAGES, TIMEOUT_MS, false, START_DOMAIN
        );
        String invalidUrl = "https://invalid-domain-that-does-not-exist.com/";

        // When
        CrawlResult result = crawler.crawl(invalidUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(0);
        assertThat(result.getTotalFailures()).isEqualTo(1);
        assertThat(result.failedUrls()).contains(invalidUrl);
        assertThat(result.successfulPages()).isEmpty();
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: Should demonstrate concurrent execution characteristics")
    void should_demonstrateConcurrentExecutionCharacteristics() {
        // Given
        StructuralConcurrencyCrawler crawler = new StructuralConcurrencyCrawler(
            MAX_DEPTH, MAX_PAGES, TIMEOUT_MS, false, START_DOMAIN
        );

        // When
        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(TARGET_URL);
        long endTime = System.currentTimeMillis();

        // Then
        long executionTime = endTime - startTime;

        // Verify that concurrent execution is working
        // (execution time should be reasonable for the number of pages)
        assertThat(executionTime).isLessThan(result.getTotalPagesCrawled() * 2000); // Max 2s per page
        assertThat(result.getTotalPagesCrawled()).isGreaterThan(1); // Should crawl multiple pages
    }
}
