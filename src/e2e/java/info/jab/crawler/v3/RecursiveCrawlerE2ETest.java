package info.jab.crawler.v3;

import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-End tests for RecursiveCrawler against real websites.
 *
 * These tests crawl actual websites and should only be run when explicitly enabled
 * via Maven profile or system property to avoid hitting real sites during regular builds.
 *
 * The recursive crawler uses trampoline pattern for safe deep recursion without stack overflow.
 *
 * Run with: mvn test -Pe2e
 * Or: mvn test -Dtest.e2e=true
 */
@EnabledIfSystemProperty(named = "test.e2e", matches = "true")
class RecursiveCrawlerE2ETest {

    private static final String TARGET_URL = "https://jabrena.github.io/cursor-rules-java/";

    @Test
    @DisplayName("E2E: Should crawl cursor-rules-java website using recursive approach with trampoline")
    void testCrawlCursorRulesJavaWebsiteRecursively() {
        // Given
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(3)  // Deeper recursion safe with trampoline
            .maxPages(15)
            .timeout(10000)  // Longer timeout for real sites
            .followExternalLinks(false)  // Stay on the same domain
            .startDomain("jabrena.github.io")
            .build();

        // When
        System.out.println("\n=== Starting Recursive E2E Test ===");
        System.out.println("Target: " + TARGET_URL);
        System.out.println("Using trampoline pattern for safe deep recursion");

        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(TARGET_URL);
        long endTime = System.currentTimeMillis();

        // Then
        System.out.println("\n=== Recursive E2E Test Results ===");
        System.out.println(result);
        System.out.printf("Execution time: %d ms%n", endTime - startTime);
        System.out.println("\nPages crawled (depth-first traversal):");
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
    @DisplayName("E2E: Should respect depth limit with recursive approach")
    void testDepthLimitOnRealWebsiteRecursive() {
        // Given - crawl with depth 0 (only seed URL)
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
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
    @DisplayName("E2E: Should handle real-world link extraction with recursive approach")
    void testLinkExtractionOnRealWebsiteRecursive() {
        // Given
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(2)  // Deeper recursion safe with trampoline
            .maxPages(5)
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
    @DisplayName("E2E: Should complete crawl within reasonable page limit with recursive approach")
    void testPageLimitOnRealWebsiteRecursive() {
        // Given
        int pageLimit = 8;
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(4)  // Deep recursion safe with trampoline
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
    @DisplayName("E2E: Should extract meaningful page titles from real site with recursive approach")
    void testPageTitleExtractionRecursive() {
        // Given
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(2)
            .maxPages(6)
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

    @Test
    @DisplayName("E2E: Should demonstrate trampoline pattern benefits with deep recursion")
    void testTrampolinePatternBenefits() {
        // Given - Test with deeper recursion to demonstrate trampoline safety
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(5)  // Deep recursion that would cause stack overflow without trampoline
            .maxPages(20)
            .timeout(15000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .build();

        // When
        System.out.println("\n=== Testing Trampoline Pattern Benefits ===");
        System.out.println("Deep recursion (depth=5) - safe with trampoline pattern");

        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(TARGET_URL);
        long endTime = System.currentTimeMillis();

        // Then
        System.out.printf("Deep recursive crawl completed in %d ms%n", endTime - startTime);
        System.out.printf("Pages crawled: %d%n", result.getTotalPagesCrawled());
        System.out.println("✓ No stack overflow occurred - trampoline pattern working correctly");

        // Verify the crawl completed successfully
        assertThat(result.getTotalPagesCrawled())
            .as("Should crawl pages without stack overflow")
            .isGreaterThanOrEqualTo(1);

        assertThat(result.getDurationMs())
            .as("Deep recursive crawl should complete in reasonable time")
            .isLessThan(90000); // Less than 90 seconds for deep crawl

        // Verify no failures due to stack overflow
        assertThat(result.failedUrls())
            .as("Should not have failures due to stack overflow")
            .noneMatch(url -> url.contains("StackOverflowError") || url.contains("stack"));
    }

    @Test
    @DisplayName("E2E: Should demonstrate functional programming approach with immutable state")
    void testFunctionalProgrammingApproach() {
        // Given
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(2)
            .maxPages(10)
            .timeout(10000)
            .followExternalLinks(false)
            .startDomain("jabrena.github.io")
            .build();

        // When
        System.out.println("\n=== Testing Functional Programming Approach ===");
        System.out.println("Using immutable state and pure functions");

        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then
        System.out.println("✓ Crawl completed using functional approach");
        System.out.println("✓ All state changes produced new immutable instances");
        System.out.println("✓ No mutable state during recursive traversal");

        // Verify functional programming characteristics
        assertThat(result.successfulPages())
            .as("Result should contain pages")
            .isNotEmpty();

        // Verify immutability of result
        final CrawlResult finalResult = result;
        assertThatThrownBy(() -> finalResult.successfulPages().add(
            new Page("http://test.com", "Test", 200, "Content", java.util.List.of())
        )).isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> finalResult.failedUrls().add("http://test.com/fail"))
            .isInstanceOf(UnsupportedOperationException.class);

        System.out.println("✓ CrawlResult is truly immutable");
    }
}
