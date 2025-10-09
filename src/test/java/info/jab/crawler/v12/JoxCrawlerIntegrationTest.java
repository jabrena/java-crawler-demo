package info.jab.crawler.v12;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Jox-based Structured Concurrency Crawler.
 *
 * These tests verify that the V12 crawler integrates correctly with the
 * builder pattern and factory methods.
 */
public class JoxCrawlerIntegrationTest {

    @Test
    public void shouldCreateV12CrawlerUsingBuilder() {
        // Given & When
        Crawler crawler = CrawlerType.JOX_STRUCTURED_CONCURRENCY
            .createBuilder()
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("example.com")
            .build();

        // Then
        assertThat(crawler).isNotNull();
        assertThat(crawler).isInstanceOf(JoxCrawler.class);
    }

    @Test
    public void shouldCreateV12CrawlerUsingDefaultFactory() {
        // Given & When
        Crawler crawler = CrawlerType.JOX_STRUCTURED_CONCURRENCY.createDefault();

        // Then
        assertThat(crawler).isNotNull();
        assertThat(crawler).isInstanceOf(JoxCrawler.class);
    }

    @Test
    public void shouldCreateV12CrawlerUsingConfigurator() {
        // Given & When
        Crawler crawler = CrawlerType.JOX_STRUCTURED_CONCURRENCY.createWith(builder -> {
            builder.maxDepth(3)
                   .maxPages(20)
                   .timeout(10000)
                   .followExternalLinks(true)
                   .startDomain("test.com");
        });

        // Then
        assertThat(crawler).isNotNull();
        assertThat(crawler).isInstanceOf(JoxCrawler.class);
    }

    @Test
    public void shouldBeAvailableInCrawlerTypeEnum() {
        // Given & When
        CrawlerType[] allTypes = CrawlerType.values();

        // Then
        assertThat(allTypes).contains(CrawlerType.JOX_STRUCTURED_CONCURRENCY);
    }
}
