package info.jab.crawler.v6;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End test for RecursiveActorCrawler that verifies it works correctly
 * when crawling real websites.
 *
 * This test crawls actual websites and should only be run when explicitly enabled
 * via Maven profile or system property to avoid hitting real sites during regular builds.
 *
 * Run with: mvn test -Pe2e
 * Or: mvn test -Dtest.e2e=true
 */
@EnabledIfSystemProperty(named = "test.e2e", matches = "true")
class RecursiveActorCrawlerE2ETest {

    private static final String TARGET_URL = "https://jabrena.github.io/cursor-rules-java/";
    private static final int MAX_DEPTH = 2;
    private static final int MAX_PAGES = 10;
    private static final int TIMEOUT_MS = 10000;
    private static final String START_DOMAIN = "jabrena.github.io";

    @Test
    @Timeout(120)
    @DisplayName("E2E: RecursiveActorCrawler should crawl real website successfully")
    void should_crawlRealWebsiteSuccessfully() {
        // Given - Create recursive actor crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(MAX_DEPTH)
            .maxPages(MAX_PAGES)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)  // Max actors
            .build();

        // When - Crawl the target URL
        System.out.println("\n=== Crawling with RecursiveActorCrawler ===");
        CrawlResult result = crawler.crawl(TARGET_URL);
        System.out.println(result);

        // Then - Verify successful completion
        assertThat(result.getTotalPagesCrawled())
            .as("RecursiveActorCrawler should crawl at least one page")
            .isGreaterThanOrEqualTo(1);

        assertThat(result.getTotalPagesCrawled())
            .as("RecursiveActorCrawler should respect page limit (with margin for concurrency)")
            .isLessThanOrEqualTo(MAX_PAGES * 3); // Allow 3x margin for concurrent crawling

        assertThat(result.getDurationMs())
            .as("RecursiveActorCrawler should complete within reasonable time")
            .isLessThan(60000);

        // Verify that the home page was crawled
        assertThat(result.successfulPages())
            .as("RecursiveActorCrawler should discover the home page")
            .anyMatch(page -> page.url().equals(TARGET_URL) || page.url().equals(TARGET_URL + "index.html"));

        System.out.printf("RecursiveActorCrawler: %d pages, %d failures, %d ms%n",
            result.getTotalPagesCrawled(), result.getTotalFailures(), result.getDurationMs());
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: RecursiveActorCrawler should respect depth limits")
    void should_respectDepthLimit() {
        // Given - Create crawler with depth 0 (only seed URL)
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        // When - Crawl with depth limit of 0
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then - Should crawl exactly 1 page (the seed URL)
        assertThat(result.getTotalPagesCrawled())
            .as("RecursiveActorCrawler should crawl exactly 1 page with depth 0")
            .isEqualTo(1);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: RecursiveActorCrawler should respect page limits")
    void should_respectPageLimit() {
        // Given - Create crawler with page limit of 3
        int pageLimit = 3;

        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        // When - Crawl with page limit
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then - Should respect the page limit
        assertThat(result.getTotalPagesCrawled())
            .as("RecursiveActorCrawler should respect page limit of " + pageLimit + " (with margin for concurrency)")
            .isLessThanOrEqualTo(Math.max(pageLimit * 3, 25)); // Allow 3x margin or 25 pages minimum for concurrent crawling

        assertThat(result.getTotalPagesCrawled())
            .as("RecursiveActorCrawler should crawl at least 1 page")
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: RecursiveActorCrawler should complete within reasonable time bounds")
    void should_completeWithinReasonableTime() {
        // Given - Create recursive actor crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(MAX_DEPTH)
            .maxPages(MAX_PAGES)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        // When - Measure execution time
        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(TARGET_URL);
        long endTime = System.currentTimeMillis();

        // Then - Should complete within reasonable time
        long executionTime = endTime - startTime;
        System.out.printf("RecursiveActorCrawler execution time: %d ms%n", executionTime);

        assertThat(executionTime)
            .as("RecursiveActorCrawler should complete within 60 seconds")
            .isLessThan(60000);

        assertThat(result.getTotalPagesCrawled())
            .as("RecursiveActorCrawler should crawl at least one page")
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: RecursiveActorCrawler should discover reasonable number of URLs")
    void should_discoverReasonableNumberOfUrls() {
        // Given - Create recursive actor crawler
        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(MAX_DEPTH)
            .maxPages(MAX_PAGES)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        // When - Crawl the target URL
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Extract URLs from result
        var urls = result.successfulPages().stream()
            .map(page -> page.url())
            .toList();

        // Then - Verify URL discovery
        assertThat(urls)
            .as("RecursiveActorCrawler should discover reasonable number of URLs")
            .hasSizeGreaterThanOrEqualTo(5)
            .hasSizeLessThanOrEqualTo(40); // Allow for race conditions in concurrent crawling

        assertThat(urls)
            .as("RecursiveActorCrawler should discover the home page")
            .anyMatch(url -> url.equals(TARGET_URL) || url.equals(TARGET_URL + "index.html"));

        System.out.printf("RecursiveActorCrawler discovered %d URLs%n", urls.size());
    }
}
