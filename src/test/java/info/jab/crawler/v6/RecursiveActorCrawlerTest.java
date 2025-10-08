package info.jab.crawler.v6;

import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.v6.ActorMessage.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RecursiveActorCrawler using mocked scenarios.
 *
 * Note: Since JSOUP's Jsoup.connect() is a static method that's hard to mock,
 * these tests focus on testing the crawler's configuration and domain models.
 * For full integration testing with mocked HTTP responses, see the WireMock tests.
 */
class RecursiveActorCrawlerTest {

    @Test
    @DisplayName("Builder should create recursive actor crawler with default values")
    void testBuilderDefaults() {
        // Given - no setup needed

        // When
        RecursiveActorCrawler crawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .build();

        // Then
        assertThat(crawler).isNotNull();
    }

    @Test
    @DisplayName("Builder should accept custom configuration for recursive actor crawler")
    void testBuilderCustomConfiguration() {
        // Given - no setup needed

        // When
        RecursiveActorCrawler crawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(5)  // Deep recursion safe with trampoline
            .maxPages(200)
            .timeout(15000)
            .followExternalLinks(true)
            .startDomain("example.com")
            .numThreads(8)  // Max actors
            .build();

        // Then
        assertThat(crawler).isNotNull();
    }

    @Test
    @DisplayName("Builder should reject negative maxDepth")
    void testBuilderRejectsNegativeMaxDepth() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(-1)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxDepth must be non-negative");
    }

    @Test
    @DisplayName("Builder should reject negative maxPages")
    void testBuilderRejectsNegativeMaxPages() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxPages(-1)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxPages must be positive");
    }

    @Test
    @DisplayName("Builder should reject negative timeout")
    void testBuilderRejectsNegativeTimeout() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .timeout(-1)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeout must be positive");
    }

    @Test
    @DisplayName("Builder should reject negative numThreads")
    void testBuilderRejectsNegativeNumThreads() {
        // Given - no setup needed

        // When & Then
        assertThatThrownBy(() -> new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .numThreads(-1)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("numThreads must be positive");
    }

    @Test
    @DisplayName("Builder should accept null startDomain")
    void testBuilderAcceptsNullStartDomain() {
        // Given - no setup needed

        // When
        RecursiveActorCrawler crawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .startDomain(null)
            .build();

        // Then
        assertThat(crawler).isNotNull();
    }

    @Test
    @DisplayName("Builder should accept blank startDomain")
    void testBuilderAcceptsBlankStartDomain() {
        // Given - no setup needed

        // When
        RecursiveActorCrawler crawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .startDomain("   ")
            .build();

        // Then
        assertThat(crawler).isNotNull();
    }

    @Test
    @DisplayName("Crawler should reject null seed URL")
    void testCrawlerRejectsNullSeedUrl() {
        // Given
        RecursiveActorCrawler crawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .build();

        // When & Then
        assertThatThrownBy(() -> crawler.crawl(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Seed URL cannot be null or empty");
    }

    @Test
    @DisplayName("Crawler should reject empty seed URL")
    void testCrawlerRejectsEmptySeedUrl() {
        // Given
        RecursiveActorCrawler crawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .build();

        // When & Then
        assertThatThrownBy(() -> crawler.crawl(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Seed URL cannot be null or empty");
    }

    @Test
    @DisplayName("Crawler should reject blank seed URL")
    void testCrawlerRejectsBlankSeedUrl() {
        // Given
        RecursiveActorCrawler crawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .build();

        // When & Then
        assertThatThrownBy(() -> crawler.crawl("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Seed URL cannot be null or empty");
    }

    @Test
    @DisplayName("ActorMessage should validate input parameters")
    void testActorMessageValidation() {
        // Test CrawlRequestMessage validation
        assertThatThrownBy(() -> new CrawlRequestMessage(null, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");

        assertThatThrownBy(() -> new CrawlRequestMessage("", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");

        assertThatThrownBy(() -> new CrawlRequestMessage("https://example.com", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Depth must be non-negative");

        // Test CrawlResultMessage validation
        assertThatThrownBy(() -> new CrawlResultMessage(null, java.util.List.of(), 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page cannot be null");

        assertThatThrownBy(() -> new CrawlResultMessage(
            new info.jab.crawler.commons.Page("https://example.com", "Test", 200, "content", java.util.List.of()),
            java.util.List.of(), -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Depth must be non-negative");

        // Test CrawlErrorMessage validation
        assertThatThrownBy(() -> new CrawlErrorMessage(null, new RuntimeException("test")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");

        assertThatThrownBy(() -> new CrawlErrorMessage("https://example.com", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Error cannot be null");

        // Test SpawnChildMessage validation
        assertThatThrownBy(() -> new SpawnChildMessage(null, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");

        assertThatThrownBy(() -> new SpawnChildMessage("https://example.com", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Depth must be non-negative");
    }

    @Test
    @DisplayName("ActorMessage should create valid instances")
    void testActorMessageCreation() {
        // Test valid message creation
        CrawlRequestMessage requestMsg = new CrawlRequestMessage("https://example.com", 1);
        assertThat(requestMsg.url()).isEqualTo("https://example.com");
        assertThat(requestMsg.depth()).isEqualTo(1);

        CrawlResultMessage resultMsg = new CrawlResultMessage(
            new info.jab.crawler.commons.Page("https://example.com", "Test", 200, "content", java.util.List.of()),
            java.util.List.of("https://example.com/page1"), 1);
        assertThat(resultMsg.page().url()).isEqualTo("https://example.com");
        assertThat(resultMsg.newLinks()).hasSize(1);
        assertThat(resultMsg.depth()).isEqualTo(1);

        CrawlErrorMessage errorMsg = new CrawlErrorMessage("https://example.com", new RuntimeException("test"));
        assertThat(errorMsg.url()).isEqualTo("https://example.com");
        assertThat(errorMsg.error()).isInstanceOf(RuntimeException.class);

        ActorCompletionMessage completionMsg = new ActorCompletionMessage();
        assertThat(completionMsg).isNotNull();

        SpawnChildMessage spawnMsg = new SpawnChildMessage("https://example.com/child", 2);
        assertThat(spawnMsg.url()).isEqualTo("https://example.com/child");
        assertThat(spawnMsg.depth()).isEqualTo(2);
    }
}
