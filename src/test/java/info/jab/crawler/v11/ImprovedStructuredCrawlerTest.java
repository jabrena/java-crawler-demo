package info.jab.crawler.v11;

import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ImprovedStructuredCrawler using mocked scenarios.
 *
 * These tests focus on testing the crawler's configuration, domain models,
 * and the improved structured concurrency features that address the SoftwareMill critique.
 */
class ImprovedStructuredCrawlerTest {

    @Test
    @DisplayName("Builder should create crawler with default values")
    void testBuilderDefaults() {
        // Given - no setup needed

        // When
        ImprovedStructuredCrawler crawler = (ImprovedStructuredCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .build();

        // Then
        assertThat(crawler).isNotNull();
    }

    @Test
    @DisplayName("Builder should accept custom configuration")
    void testBuilderCustomConfiguration() {
        // Given - no setup needed

        // When
        ImprovedStructuredCrawler crawler = (ImprovedStructuredCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .maxDepth(3)
            .maxPages(100)
            .timeout(10000)
            .followExternalLinks(true)
            .startDomain("example.com")
            .build();

        // Then
        assertThat(crawler).isNotNull();
    }

    @Test
    @DisplayName("Builder should reject negative maxDepth")
    void testBuilderRejectsNegativeMaxDepth() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY).maxDepth(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Builder should reject zero or negative maxPages")
    void testBuilderRejectsInvalidMaxPages() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY).maxPages(0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY).maxPages(-10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Builder should reject zero or negative timeout")
    void testBuilderRejectsInvalidTimeout() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY).timeout(0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY).timeout(-5000))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CrawlTask should validate URL and depth")
    void testCrawlTaskValidation() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new CrawlTask(null, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("URL cannot be null or empty");

        assertThatThrownBy(() -> new CrawlTask("", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("URL cannot be null or empty");

        assertThatThrownBy(() -> new CrawlTask("   ", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("URL cannot be null or empty");

        assertThatThrownBy(() -> new CrawlTask("http://example.com", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Depth cannot be negative");
    }

    @Test
    @DisplayName("CrawlTask should create next depth task correctly")
    void testCrawlTaskNextDepth() {
        // Given
        CrawlTask task = new CrawlTask("http://example.com", 2);

        // When
        CrawlTask nextTask = task.nextDepth();

        // Then
        assertThat(nextTask.url()).isEqualTo("http://example.com");
        assertThat(nextTask.depth()).isEqualTo(3);
    }

    @Test
    @DisplayName("CrawlTask should check max depth correctly")
    void testCrawlTaskMaxDepth() {
        // Given
        CrawlTask taskAtMax = new CrawlTask("http://example.com", 5);
        CrawlTask taskBelowMax = new CrawlTask("http://example.com", 3);

        // When & Then
        assertThat(taskAtMax.isAtMaxDepth(5)).isTrue();
        assertThat(taskAtMax.isAtMaxDepth(4)).isTrue();
        assertThat(taskAtMax.isAtMaxDepth(6)).isFalse();

        assertThat(taskBelowMax.isAtMaxDepth(5)).isFalse();
        assertThat(taskBelowMax.isAtMaxDepth(3)).isTrue();
    }

    @Test
    @DisplayName("UnifiedCancellationJoiner should track completion state")
    void testUnifiedCancellationJoinerCompletion() {
        // Given
        UnifiedCancellationJoiner<String> joiner = new UnifiedCancellationJoiner<>();

        // When & Then
        assertThat(joiner.getFirstException()).isNull();
    }

    @Test
    @DisplayName("Page record should validate URL")
    void testPageRecordValidation() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new Page(null, "Title", 200, "Content", java.util.List.of()))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Page("", "Title", 200, "Content", java.util.List.of()))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Page("   ", "Title", 200, "Content", java.util.List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Page should correctly identify successful status codes")
    void testPageIsSuccessful() {
        // Given
        Page successPage = new Page("http://test.com", "Test", 200, "Content", java.util.List.of());
        Page redirectPage = new Page("http://test.com", "Test", 301, "Content", java.util.List.of());
        Page errorPage = new Page("http://test.com", "Test", 404, "Content", java.util.List.of());

        // When & Then
        assertThat(successPage.isSuccessful()).isTrue();
        assertThat(redirectPage.isSuccessful()).isFalse();
        assertThat(errorPage.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("CrawlResult should track successful pages")
    void testCrawlResultTracksSuccess() {
        // Given
        CrawlResult result = CrawlResult.empty();
        Page page1 = new Page("http://test.com/page1", "Page 1", 200, "Content 1", java.util.List.of());
        Page page2 = new Page("http://test.com/page2", "Page 2", 200, "Content 2", java.util.List.of());

        // When
        result = result.withSuccessfulPage(page1);
        result = result.withSuccessfulPage(page2);

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(2);
        assertThat(result.successfulPages()).hasSize(2);
    }

    @Test
    @DisplayName("CrawlResult should track failed URLs")
    void testCrawlResultTracksFailures() {
        // Given
        CrawlResult result = CrawlResult.empty();

        // When
        result = result.withFailedUrl("http://test.com/fail1");
        result = result.withFailedUrl("http://test.com/fail2");

        // Then
        assertThat(result.getTotalFailures()).isEqualTo(2);
        assertThat(result.failedUrls()).hasSize(2);
    }

    @Test
    @DisplayName("CrawlResult should calculate duration after marking complete")
    void testCrawlResultDuration() throws InterruptedException {
        // Given
        CrawlResult result = CrawlResult.empty();

        // When
        Thread.sleep(100); // Simulate some crawling time
        result = result.markComplete();

        // Then
        assertThat(result.getDurationMs()).isGreaterThanOrEqualTo(100);
    }

    @Test
    @DisplayName("CrawlResult should provide immutable collections")
    void testCrawlResultImmutability() {
        // Given
        Page page = new Page("http://test.com", "Test", 200, "Content", java.util.List.of());
        final CrawlResult result = CrawlResult.empty().withSuccessfulPage(page);

        // When & Then
        assertThatThrownBy(() -> result.successfulPages().add(page))
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> result.failedUrls().add("http://test.com/fail"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("CrawlResult toString should contain summary information")
    void testCrawlResultToString() {
        // Given
        CrawlResult result = CrawlResult.empty();
        Page page = new Page("http://test.com", "Test", 200, "Content", java.util.List.of());

        // When
        result = result.withSuccessfulPage(page);
        result = result.withFailedUrl("http://test.com/fail");
        result = result.markComplete();

        // Then
        String summary = result.toString();
        assertThat(summary)
            .contains("successful=1")
            .contains("failed=1")
            .contains("duration=");
    }

    @Test
    @DisplayName("ImprovedStructuredCrawler should reject null seed URL")
    void testImprovedStructuredCrawlerRejectsNullSeedUrl() {
        // Given
        ImprovedStructuredCrawler crawler = (ImprovedStructuredCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .build();

        // When & Then
        assertThatThrownBy(() -> crawler.crawl(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Seed URL cannot be null or empty");

        assertThatThrownBy(() -> crawler.crawl(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Seed URL cannot be null or empty");

        assertThatThrownBy(() -> crawler.crawl("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Seed URL cannot be null or empty");
    }
}
