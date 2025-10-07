package info.jab.crawler.v3;

import info.jab.crawler.commons.Page;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RecursiveCrawler using WireMock to simulate a real website.
 *
 * This test simulates a small 3-page website:
 * - /index.html (links to /about and /contact)
 * - /about.html (links to /contact)
 * - /contact.html (no outgoing links)
 *
 * The recursive crawler uses depth-first traversal with trampoline pattern
 * to safely handle deep recursion without stack overflow.
 */
class RecursiveCrawlerIT {

    private WireMockServer wireMockServer;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        // Start WireMock server on random port with response templating enabled
        wireMockServer = new WireMockServer(
            wireMockConfig()
                .dynamicPort()
                .withRootDirectory("src/test/resources")
        );
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        baseUrl = "http://localhost:" + wireMockServer.port();

        // Set up the 3-page mock website
        setupMockWebsite();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    private void setupMockWebsite() {
        // Page 1: Index page with links to about and contact
        stubFor(get(urlEqualTo("/index.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("index.html")
                .withTransformers("response-template")));

        // Page 2: About page with link to contact
        stubFor(get(urlEqualTo("/about.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("about.html")
                .withTransformers("response-template")));

        // Page 3: Contact page with no outgoing links
        stubFor(get(urlEqualTo("/contact.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("contact.html")));
    }

    @Test
    @DisplayName("Should crawl all 3 pages using recursive approach with trampoline")
    void testCrawlAllPagesRecursively() {
        // Given
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3);
        assertThat(result.getTotalFailures()).isEqualTo(0);
        assertThat(result.getDurationMs()).isGreaterThan(0);

        // Verify all pages were crawled
        assertThat(result.successfulPages())
            .hasSize(3)
            .extracting(Page::url)
            .containsExactlyInAnyOrder(
                baseUrl + "/index.html",
                baseUrl + "/about.html",
                baseUrl + "/contact.html"
            );

        // Verify page titles
        assertThat(result.successfulPages())
            .extracting(Page::title)
            .containsExactlyInAnyOrder(
                "Home Page",
                "About Page",
                "Contact Page"
            );

        // Verify all pages have successful status
        assertThat(result.successfulPages())
            .allMatch(Page::isSuccessful);

        // Verify WireMock received the requests
        verify(exactly(1), getRequestedFor(urlEqualTo("/index.html")));
        verify(exactly(1), getRequestedFor(urlEqualTo("/about.html")));
        verify(exactly(1), getRequestedFor(urlEqualTo("/contact.html")));
    }

    @Test
    @DisplayName("Should respect maxPages limit with recursive approach")
    void testMaxPagesLimitRecursive() {
        // Given
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(2)
            .maxPages(2)  // Limit to 2 pages
            .timeout(5000)
            .followExternalLinks(true)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should respect maxDepth limit with recursive approach")
    void testMaxDepthLimitRecursive() {
        // Given
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(0)  // Only crawl the seed URL
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1);
        assertThat(result.successfulPages().get(0).url())
            .isEqualTo(baseUrl + "/index.html");
    }

    @Test
    @DisplayName("Should handle deep recursion safely with trampoline")
    void testDeepRecursionSafety() {
        // Given - Create a deeper website structure
        setupDeepWebsite();

        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(5)  // Deep recursion - safe with trampoline
            .maxPages(20)
            .timeout(5000)
            .followExternalLinks(true)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/level1.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(6); // level1 through level6
        assertThat(result.getTotalFailures()).isEqualTo(0);

        // Verify all levels were crawled
        for (int i = 1; i <= 6; i++) {
            assertThat(result.successfulPages())
                .extracting(Page::url)
                .contains(baseUrl + "/level" + i + ".html");
        }
    }

    @Test
    @DisplayName("Should extract correct number of links from each page")
    void testLinkExtractionRecursive() {
        // Given
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        Page indexPage = result.successfulPages().stream()
            .filter(p -> p.url().endsWith("/index.html"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Index page not found"));
        assertThat(indexPage.links()).hasSize(2);

        Page aboutPage = result.successfulPages().stream()
            .filter(p -> p.url().endsWith("/about.html"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("About page not found"));
        assertThat(aboutPage.links()).hasSize(1);

        Page contactPage = result.successfulPages().stream()
            .filter(p -> p.url().endsWith("/contact.html"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Contact page not found"));
        assertThat(contactPage.links()).isEmpty();
    }

    @Test
    @DisplayName("Should handle 404 errors gracefully with recursive approach")
    void testHandles404ErrorsRecursive() {
        // Given - add a broken link to index page
        stubFor(get(urlEqualTo("/index-with-404.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("index-with-404.html")
                .withTransformers("response-template")));

        stubFor(get(urlEqualTo("/broken.html"))
            .willReturn(aResponse().withStatus(404)));

        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(1)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index-with-404.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1);
        assertThat(result.getTotalFailures()).isEqualTo(1);
        assertThat(result.failedUrls()).contains(baseUrl + "/broken.html");
    }

    @Test
    @DisplayName("Should avoid duplicate crawling of same URL with recursive approach")
    void testAvoidsDuplicatesRecursive() {
        // Given - Create a page with duplicate links
        stubFor(get(urlEqualTo("/duplicates.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("duplicates.html")
                .withTransformers("response-template")));

        stubFor(get(urlEqualTo("/target.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("target.html")));

        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(1)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/duplicates.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(2);

        // Verify target was only requested once
        verify(exactly(1), getRequestedFor(urlEqualTo("/target.html")));
    }

    @Test
    @DisplayName("Should extract page content correctly with recursive approach")
    void testContentExtractionRecursive() {
        // Given
        RecursiveCrawler crawler = (RecursiveCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(0)
            .maxPages(1)
            .timeout(5000)
            .followExternalLinks(true)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        Page page = result.successfulPages().get(0);
        assertThat(page.content())
            .contains("Welcome to Test Site")
            .contains("This is the home page");
    }

    /**
     * Sets up a deeper website structure for testing deep recursion safety.
     */
    private void setupDeepWebsite() {
        // Create 6 levels of pages, each linking to the next
        for (int i = 1; i <= 6; i++) {
            String currentLevel = "/level" + i + ".html";
            String nextLevel = i < 6 ? "/level" + (i + 1) + ".html" : "";

            String htmlContent = String.format(
                "<html><head><title>Level %d Page</title></head><body>" +
                "<h1>Level %d</h1>" +
                "<p>This is level %d of the deep website.</p>" +
                (i < 6 ? "<a href=\"%s\">Go to Level %d</a>" : "") +
                "</body></html>",
                i, i, i, nextLevel, i + 1
            );

            stubFor(get(urlEqualTo(currentLevel))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/html")
                    .withBody(htmlContent)));
        }
    }
}
