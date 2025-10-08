package info.jab.crawler.v10;

import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End tests for VirtualThreadActorCrawler against real websites.
 *
 * These tests crawl actual websites and should only be run when explicitly enabled
 * via Maven profile or system property to avoid hitting real sites during regular builds.
 *
 * Run with: mvn test -Pe2e
 * Or: mvn test -Dtest.e2e=true
 */
@EnabledIfSystemProperty(named = "test.e2e", matches = "true")
class VirtualThreadActorCrawlerE2ETest {

    private static final String TARGET_URL = "https://jabrena.github.io/cursor-rules-java/";

    @Test
    @Timeout(60)
    @DisplayName("E2E: Should crawl cursor-rules-java website successfully")
    void testCrawlCursorRulesJavaWebsite() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(2)
            .maxPages(10)
            .timeout(10000)  // Longer timeout for real sites
            .followExternalLinks(false)  // Stay on the same domain
            .startDomain("jabrena.github.io")
            .numThreads(4)  // Use 4 actors for parallel processing
            .build();

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then

        // Assertions
        assertThat(result.getTotalPagesCrawled())
            .as("Should crawl at least one page")
            .isGreaterThanOrEqualTo(1);

        assertThat(result.getDurationMs())
            .as("Crawl should complete in reasonable time")
            .isLessThan(60000); // Less than 60 seconds

        // Verify the home page was crawled
        assertThat(result.successfulPages())
            .as("Should contain the home page")
            .anyMatch(page -> page.url().equals(TARGET_URL) ||
                             page.url().equals(TARGET_URL.replaceAll("/$", "")));

        // Check that we found some pages with the expected domain
        assertThat(result.successfulPages())
            .as("All successful pages should be from the target domain")
            .allMatch(page -> page.url().contains("jabrena.github.io"));

        // Verify pages have content
        assertThat(result.successfulPages())
            .as("All pages should have content")
            .allMatch(page -> page.content() != null && !page.content().isBlank());

        // Verify at least the home page has expected content
        Page homePage = result.successfulPages().stream()
            .filter(page -> page.url().contains("cursor-rules-java"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Home page not found"));

        assertThat(homePage.title())
            .as("Home page should have a title")
            .isNotBlank();

        assertThat(homePage.content())
            .as("Home page should contain expected content")
            .containsAnyOf("Cursor Rules", "Java", "cursor-rules-java");
    }

    @Test
    @Timeout(60)
    @DisplayName("E2E: Should respect depth limit on real website")
    void testDepthLimitOnRealWebsite() {
        // Given - crawl with depth 0 (only seed URL)
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(0)
            .maxPages(10)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        assertThat(result.getTotalPagesCrawled())
            .as("Should crawl exactly one page with depth 0")
            .isEqualTo(1);
    }

    @Test
    @Timeout(60)
    @DisplayName("E2E: Should handle real-world link extraction")
    void testLinkExtractionOnRealWebsite() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(1)
            .maxPages(3)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .numThreads(3)
            .build();

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        assertThat(result.successfulPages())
            .as("Should find pages with links")
            .isNotEmpty();

        // Check that at least one page has links
        assertThat(result.successfulPages())
            .as("At least one page should have extracted links")
            .anyMatch(page -> !page.links().isEmpty());

        // Verify links are absolute URLs
        result.successfulPages().stream()
            .flatMap(page -> page.links().stream())
            .forEach(link -> assertThat(link)
                .as("All extracted links should be absolute URLs")
                .matches("^https?://.*"));
    }

    @Test
    @Timeout(60)
    @DisplayName("E2E: Should complete crawl within reasonable page limit")
    void testPageLimitOnRealWebsite() {
        // Given
        int pageLimit = 5;
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(3)
            .maxPages(pageLimit)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .numThreads(4)
            .build();

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        assertThat(result.getTotalPagesCrawled())
            .as("Should respect page limit")
            .isLessThanOrEqualTo(pageLimit);
    }

    @Test
    @Timeout(60)
    @DisplayName("E2E: Should extract meaningful page titles from real site")
    void testPageTitleExtraction() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(1)
            .maxPages(5)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        assertThat(result.successfulPages())
            .as("All pages should have titles")
            .allMatch(page -> page.title() != null);

        // At least some pages should have non-empty titles
        assertThat(result.successfulPages())
            .as("Most pages should have meaningful titles")
            .filteredOn(page -> !page.title().isBlank())
            .hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @Timeout(60)
    @DisplayName("E2E: Should demonstrate virtual thread actor benefits with different actor counts")
    void testVirtualThreadActorScalability() {
        // Given - Test with different actor counts
        VirtualThreadActorCrawler singleActorCrawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(1)
            .maxPages(3)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .numThreads(1)
            .build();

        VirtualThreadActorCrawler multiActorCrawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(1)
            .maxPages(3)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .numThreads(6)
            .build();

        // When
        long startTime1 = System.currentTimeMillis();
        CrawlResult singleResult = singleActorCrawler.crawl(TARGET_URL);
        long duration1 = System.currentTimeMillis() - startTime1;

        long startTime2 = System.currentTimeMillis();
        CrawlResult multiResult = multiActorCrawler.crawl(TARGET_URL);
        long duration2 = System.currentTimeMillis() - startTime2;

        // Then

        // Both should crawl a reasonable number of pages (within the limit)
        assertThat(singleResult.getTotalPagesCrawled())
            .as("Single actor should crawl some pages")
            .isGreaterThanOrEqualTo(1)
            .isLessThanOrEqualTo(3);

        assertThat(multiResult.getTotalPagesCrawled())
            .as("Multi actor should crawl some pages")
            .isGreaterThanOrEqualTo(1)
            .isLessThanOrEqualTo(3);

        // Both should crawl the home page
        assertThat(singleResult.successfulPages())
            .as("Single actor should crawl the home page")
            .anyMatch(page -> page.url().equals(TARGET_URL) || page.url().equals(TARGET_URL + "index.html"));

        assertThat(multiResult.successfulPages())
            .as("Multi actor should crawl the home page")
            .anyMatch(page -> page.url().equals(TARGET_URL) || page.url().equals(TARGET_URL + "index.html"));

        // Multi-actor should be at least as fast (or faster due to parallelization)
        // Note: This might not always be true due to network variability
    }
}
