package info.jab.crawler.v1;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End tests for SequentialCrawler against real websites.
 *
 * These tests crawl actual websites and should only be run when explicitly enabled
 * via Maven profile or system property to avoid hitting real sites during regular builds.
 *
 * Run with: mvn test -Pe2e
 * Or: mvn test -Dtest.e2e=true
 */
@EnabledIfSystemProperty(named = "test.e2e", matches = "true")
class SequentialCrawlerE2ETest {

    private static final String TARGET_URL = "https://jabrena.github.io/cursor-rules-java/";

    @Test
    @DisplayName("E2E: Should crawl cursor-rules-java website successfully")
    void testCrawlCursorRulesJavaWebsite() {
        // Given
        SequentialCrawler crawler = new SequentialCrawler.Builder()
            .maxDepth(2)
            .maxPages(10)
            .timeout(10000)  // Longer timeout for real sites
            .followExternalLinks(false)  // Stay on the same domain
            .startDomain("jabrena.github.io")
            .build();

        // When
        System.out.println("\n=== Starting E2E Test ===");
        System.out.println("Target: " + TARGET_URL);

        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        System.out.println("\n=== E2E Test Results ===");
        System.out.println(result);
        System.out.println("\nPages crawled:");
        result.successfulPages().forEach(page ->
            System.out.printf("  - %s (title: %s, links: %d)%n",
                page.url(), page.title(), page.links().size())
        );

        if (!result.failedUrls().isEmpty()) {
            System.out.println("\nFailed URLs:");
            result.failedUrls().forEach(url -> System.out.println("  - " + url));
        }

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
    @DisplayName("E2E: Should respect depth limit on real website")
    void testDepthLimitOnRealWebsite() {
        // Given - crawl with depth 0 (only seed URL)
        SequentialCrawler crawler = new SequentialCrawler.Builder()
            .maxDepth(0)
            .maxPages(10)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .build();

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        assertThat(result.getTotalPagesCrawled())
            .as("Should crawl exactly one page with depth 0")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("E2E: Should handle real-world link extraction")
    void testLinkExtractionOnRealWebsite() {
        // Given
        SequentialCrawler crawler = new SequentialCrawler.Builder()
            .maxDepth(1)
            .maxPages(3)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
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
    @DisplayName("E2E: Should complete crawl within reasonable page limit")
    void testPageLimitOnRealWebsite() {
        // Given
        int pageLimit = 5;
        SequentialCrawler crawler = new SequentialCrawler.Builder()
            .maxDepth(3)
            .maxPages(pageLimit)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .build();

        // When
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        assertThat(result.getTotalPagesCrawled())
            .as("Should respect page limit")
            .isLessThanOrEqualTo(pageLimit);
    }

    @Test
    @DisplayName("E2E: Should extract meaningful page titles from real site")
    void testPageTitleExtraction() {
        // Given
        SequentialCrawler crawler = new SequentialCrawler.Builder()
            .maxDepth(1)
            .maxPages(5)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
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
}

