package info.jab.crawler.v8;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Hybrid Actor-Structural Concurrency Crawler.
 *
 * These tests verify that the V8 crawler integrates correctly with the
 * builder pattern and factory methods.
 */
public class HybridActorStructuralCrawlerIntegrationTest {

    @Test
    public void shouldCreateV8CrawlerUsingBuilder() {
        // Given & When
        Crawler crawler = CrawlerType.HYBRID_ACTOR_STRUCTURAL
            .createBuilder()
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("example.com")
            .numThreads(4)
            .build();

        // Then
        assertThat(crawler).isNotNull();
        assertThat(crawler).isInstanceOf(HybridActorStructuralCrawler.class);
    }

    @Test
    public void shouldCreateV8CrawlerUsingDefaultFactory() {
        // Given & When
        Crawler crawler = CrawlerType.HYBRID_ACTOR_STRUCTURAL.createDefault();

        // Then
        assertThat(crawler).isNotNull();
        assertThat(crawler).isInstanceOf(HybridActorStructuralCrawler.class);
    }

    @Test
    public void shouldCreateV8CrawlerUsingConfigurator() {
        // Given & When
        Crawler crawler = CrawlerType.HYBRID_ACTOR_STRUCTURAL.createWith(builder -> {
            builder.maxDepth(3)
                   .maxPages(20)
                   .timeout(10000)
                   .numThreads(6);
        });

        // Then
        assertThat(crawler).isNotNull();
        assertThat(crawler).isInstanceOf(HybridActorStructuralCrawler.class);
    }

    @Test
    public void shouldBeAvailableInCrawlerTypeEnum() {
        // Given & When
        CrawlerType[] allTypes = CrawlerType.values();

        // Then
        assertThat(allTypes).contains(CrawlerType.HYBRID_ACTOR_STRUCTURAL);
    }
}
