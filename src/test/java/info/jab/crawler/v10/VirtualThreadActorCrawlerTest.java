package info.jab.crawler.v10;

import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.v10.Message.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for VirtualThreadActorCrawler using mocked scenarios.
 *
 * Note: Since JSOUP's Jsoup.connect() is a static method that's hard to mock,
 * these tests focus on testing the crawler's configuration and domain models.
 * For full integration testing with mocked HTTP responses, see the WireMock tests.
 */
class VirtualThreadActorCrawlerTest {

    @Test
    @DisplayName("Builder should create crawler with default values")
    void testBuilderDefaults() {
        // Given - no setup needed

        // When
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .build();

        // Then
        assertThat(crawler).isNotNull();
    }

    @Test
    @DisplayName("Builder should accept custom configuration")
    void testBuilderCustomConfiguration() {
        // Given - no setup needed

        // When
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(3)
            .maxPages(100)
            .timeout(10000)
            .followExternalLinks(true)
            .startDomain("example.com")
            .numThreads(8)
            .build();

        // Then
        assertThat(crawler).isNotNull();
    }

    @Test
    @DisplayName("Builder should reject negative maxDepth")
    void testBuilderRejectsNegativeMaxDepth() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR).maxDepth(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Builder should reject zero or negative maxPages")
    void testBuilderRejectsInvalidMaxPages() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR).maxPages(0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR).maxPages(-10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Builder should reject zero or negative timeout")
    void testBuilderRejectsInvalidTimeout() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR).timeout(0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR).timeout(-5000))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Builder should reject zero or negative numThreads")
    void testBuilderRejectsInvalidNumThreads() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR).numThreads(0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR).numThreads(-5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CrawlMessage should validate URL")
    void testCrawlMessageValidation() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new CrawlMessage(null, 0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CrawlMessage("", 0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CrawlMessage("   ", 0))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CrawlMessage("http://test.com", -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ResultMessage should validate page")
    void testResultMessageValidation() {
        // Given
        Page validPage = new Page("http://test.com", "Test", 200, "Content", java.util.List.of());

        // When & Then
        assertThatThrownBy(() -> new ResultMessage(null, java.util.List.of(), 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ErrorMessage should validate parameters")
    void testErrorMessageValidation() {
        // Given
        Exception validException = new RuntimeException("Test error");

        // When & Then
        assertThatThrownBy(() -> new ErrorMessage(null, validException))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ErrorMessage("", validException))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ErrorMessage("http://test.com", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Messages should be immutable")
    void testMessageImmutability() {
        // Given
        CrawlMessage crawlMsg = new CrawlMessage("http://test.com", 1);
        Page page = new Page("http://test.com", "Test", 200, "Content", java.util.List.of());
        ResultMessage resultMsg = new ResultMessage(page, java.util.List.of("http://link1.com"), 0);
        ErrorMessage errorMsg = new ErrorMessage("http://test.com", new RuntimeException("Error"));
        CompletionMessage completionMsg = new CompletionMessage();

        // When & Then - all messages should be immutable
        assertThat(crawlMsg.url()).isEqualTo("http://test.com");
        assertThat(crawlMsg.depth()).isEqualTo(1);
        assertThat(resultMsg.page()).isEqualTo(page);
        assertThat(resultMsg.newLinks()).hasSize(1);
        assertThat(errorMsg.url()).isEqualTo("http://test.com");
        assertThat(errorMsg.error()).isInstanceOf(RuntimeException.class);
        assertThat(completionMsg).isNotNull();
    }

    @Test
    @DisplayName("ResultMessage should provide immutable links list")
    void testResultMessageLinksImmutability() {
        // Given
        Page page = new Page("http://test.com", "Test", 200, "Content", java.util.List.of());
        java.util.List<String> originalLinks = new java.util.ArrayList<>();
        originalLinks.add("http://link1.com");
        originalLinks.add("http://link2.com");
        ResultMessage resultMsg = new ResultMessage(page, originalLinks, 0);

        // When & Then
        assertThatThrownBy(() -> resultMsg.newLinks().add("http://link3.com"))
            .isInstanceOf(UnsupportedOperationException.class);

        // Original list should be unchanged
        assertThat(originalLinks).hasSize(2);
    }

    @Test
    @DisplayName("VirtualThreadActorCrawler should reject null seed URL")
    void testVirtualThreadActorCrawlerRejectsNullSeedUrl() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .build();

        // When & Then
        assertThatThrownBy(() -> crawler.crawl(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("VirtualThreadActorCrawler should reject empty seed URL")
    void testVirtualThreadActorCrawlerRejectsEmptySeedUrl() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .build();

        // When & Then
        assertThatThrownBy(() -> crawler.crawl(""))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> crawler.crawl("   "))
            .isInstanceOf(IllegalArgumentException.class);
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
}
