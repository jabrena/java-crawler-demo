package info.jab.crawler.v3;

import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RecursiveCrawler using mocked scenarios.
 *
 * Note: Since JSOUP's Jsoup.connect() is a static method that's hard to mock,
 * these tests focus on testing the crawler's configuration and domain models.
 * For full integration testing with mocked HTTP responses, see the WireMock tests.
 */
class RecursiveCrawlerTest {

    @Test
    @DisplayName("Builder should create recursive crawler with default values")
    void testBuilderDefaults() {
        // Given - no setup needed

        // When
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .build();

        // Then
        assertThat(crawler).isNotNull();
    }

    @Test
    @DisplayName("Builder should accept custom configuration for recursive crawler")
    void testBuilderCustomConfiguration() {
        // Given - no setup needed

        // When
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(5)  // Deep recursion safe with trampoline
            .maxPages(200)
            .timeout(15000)
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
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.RECURSIVE).maxDepth(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Builder should reject zero or negative maxPages")
    void testBuilderRejectsInvalidMaxPages() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.RECURSIVE).maxPages(0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.RECURSIVE).maxPages(-10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Builder should reject zero or negative timeout")
    void testBuilderRejectsInvalidTimeout() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.RECURSIVE).timeout(0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.RECURSIVE).timeout(-5000))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Trampoline should handle simple completion")
    void testTrampolineSimpleCompletion() {
        // Given
        Trampoline<String> trampoline = Trampoline.done("Hello World");

        // When
        String result = trampoline.run();

        // Then
        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("Trampoline should handle continuation")
    void testTrampolineContinuation() {
        // Given
        Trampoline<Integer> trampoline = Trampoline.more(() ->
            Trampoline.more(() ->
                Trampoline.done(42)
            )
        );

        // When
        Integer result = trampoline.run();

        // Then
        assertThat(result).isEqualTo(42);
    }

    @Test
    @DisplayName("Trampoline should handle deep recursion safely")
    void testTrampolineDeepRecursion() {
        // Given - create a deep recursive trampoline
        Trampoline<Integer> deepTrampoline = createDeepTrampoline(1000);

        // When
        Integer result = deepTrampoline.run();

        // Then
        assertThat(result).isEqualTo(1000);
    }

    @Test
    @DisplayName("Trampoline should respect step limit")
    void testTrampolineStepLimit() {
        // Given
        Trampoline<Integer> infiniteTrampoline = Trampoline.more(() ->
            Trampoline.more(() ->
                Trampoline.more(() ->
                    Trampoline.done(42)
                )
            )
        );

        // When & Then
        assertThatThrownBy(() -> infiniteTrampoline.runWithLimit(2))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("exceeded maximum steps");
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

    /**
     * Helper method to create a deep recursive trampoline for testing.
     */
    private Trampoline<Integer> createDeepTrampoline(int depth) {
        if (depth <= 0) {
            return Trampoline.done(0);
        }

        return Trampoline.more(() -> {
            Trampoline<Integer> next = createDeepTrampoline(depth - 1);
            return next.run() == depth - 1 ? Trampoline.done(depth) : next;
        });
    }
}
