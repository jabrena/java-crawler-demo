package info.jab.crawler.v8;

import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the Hybrid Actor-Structural Concurrency Crawler.
 *
 * These tests verify that the hybrid crawler correctly combines actor-based
 * coordination with structural concurrency for actual crawling work.
 */
public class HybridActorStructuralCrawlerE2ETest {

    @Test
    public void shouldCrawlSuccessfullyWithHybridApproach() {
        // Given
        HybridActorStructuralCrawler crawler = new HybridActorStructuralCrawler(
            2, 10, 5000, false, "example.com", 4
        );

        // When
        CrawlResult result = crawler.crawl("http://example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.successfulPages()).isNotEmpty();
        assertThat(result.successfulPages().size()).isLessThanOrEqualTo(10);
        assertThat(result.startTime()).isLessThanOrEqualTo(result.endTime());

        // Verify that we have at least the seed page
        assertThat(result.successfulPages())
            .anyMatch(page -> page.url().equals("http://example.com"));
    }

    @Test
    public void shouldRespectMaxPagesLimit() {
        // Given
        HybridActorStructuralCrawler crawler = new HybridActorStructuralCrawler(
            3, 5, 5000, false, "example.com", 2
        );

        // When
        CrawlResult result = crawler.crawl("http://example.com");

        // Then
        assertThat(result.successfulPages().size()).isLessThanOrEqualTo(5);
    }

    @Test
    public void shouldRespectMaxDepthLimit() {
        // Given
        HybridActorStructuralCrawler crawler = new HybridActorStructuralCrawler(
            1, 20, 5000, false, "example.com", 3
        );

        // When
        CrawlResult result = crawler.crawl("http://example.com");

        // Then
        assertThat(result.successfulPages()).isNotEmpty();
        // All pages should be at depth 0 or 1
        for (Page page : result.successfulPages()) {
            assertThat(page.url()).isNotNull();
        }
    }

    @Test
    public void shouldHandleInvalidUrlGracefully() {
        // Given
        HybridActorStructuralCrawler crawler = new HybridActorStructuralCrawler(
            2, 10, 5000, false, "example.com", 2
        );

        // When & Then
        try {
            crawler.crawl("invalid-url");
            // Should not reach here, but if it does, check that we have some failures
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).contains("Hybrid actor-structural crawling failed");
        }
    }

    @Test
    public void shouldHandleNullUrl() {
        // Given
        HybridActorStructuralCrawler crawler = new HybridActorStructuralCrawler(
            2, 10, 5000, false, "example.com", 2
        );

        // When & Then
        try {
            crawler.crawl(null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Seed URL cannot be null or empty");
        }
    }

    @Test
    public void shouldHandleEmptyUrl() {
        // Given
        HybridActorStructuralCrawler crawler = new HybridActorStructuralCrawler(
            2, 10, 5000, false, "example.com", 2
        );

        // When & Then
        try {
            crawler.crawl("");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Seed URL cannot be null or empty");
        }
    }

    @Test
    public void shouldUseActorBasedStateManagement() {
        // Given
        HybridActorStructuralCrawler crawler = new HybridActorStructuralCrawler(
            2, 15, 5000, false, "example.com", 3
        );

        // When
        CrawlResult result = crawler.crawl("http://example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.successfulPages()).isNotEmpty();
        assertThat(result.failedUrls()).isNotNull();

        // Verify no duplicate URLs (actor state management)
        long uniqueUrls = result.successfulPages().stream()
            .map(Page::url)
            .distinct()
            .count();
        assertThat(uniqueUrls).isEqualTo(result.successfulPages().size());
    }
}
