package info.jab.crawler.v11;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End tests for ImprovedStructuredCrawler using WireMock.
 *
 * These tests verify the improved structured concurrency features that address
 * the SoftwareMill critique, including uniform cancellation, unified scope logic,
 * and timeout-as-method patterns.
 */
class ImprovedStructuredCrawlerE2ETest {

    private WireMockServer wireMockServer;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8080);
        baseUrl = "http://localhost:8080";
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @Timeout(30)
    @DisplayName("E2E: Should crawl multiple pages successfully with improved structured concurrency")
    void should_crawlMultiplePagesSuccessfully_when_usingImprovedStructuredConcurrency() {
        // Given - Setup mock responses
        setupMockResponses();

        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("localhost")
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isGreaterThanOrEqualTo(1);
        assertThat(result.successfulPages()).hasSize(result.getTotalPagesCrawled());
        assertThat(result.getTotalFailures()).isEqualTo(0);
        assertThat(result.getDurationMs()).isGreaterThan(0);

        // Verify specific pages were crawled
        assertThat(result.successfulPages().stream()
            .anyMatch(page -> page.url().contains("/index.html"))).isTrue();
    }

    @Test
    @Timeout(30)
    @DisplayName("E2E: Should respect depth limits with improved structured concurrency")
    void should_respectDepthLimits_when_usingImprovedStructuredConcurrency() {
        // Given - Setup mock responses
        setupMockResponses();

        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .maxDepth(1) // Limit to depth 1
            .maxPages(20)
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("localhost")
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isGreaterThanOrEqualTo(1);
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(4); // Should not go deeper than depth 1
        assertThat(result.getTotalFailures()).isEqualTo(0);
    }

    @Test
    @Timeout(30)
    @DisplayName("E2E: Should respect page limits with improved structured concurrency")
    void should_respectPageLimits_when_usingImprovedStructuredConcurrency() {
        // Given - Setup mock responses
        setupMockResponses();

        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .maxDepth(3)
            .maxPages(2) // Limit to 2 pages
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("localhost")
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(2);
        assertThat(result.getTotalFailures()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Timeout(30)
    @DisplayName("E2E: Should handle timeout errors with timeout-as-method pattern")
    void should_handleTimeoutErrors_when_usingTimeoutAsMethodPattern() {
        // Given - Setup slow response
        stubFor(get(urlEqualTo("/slow.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Slow Page</title></head><body>Slow content</body></html>")
                .withFixedDelay(10000))); // 10 second delay

        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .maxDepth(1)
            .maxPages(5)
            .timeout(1000) // 1 second timeout
            .followExternalLinks(false)
            .startDomain("localhost")
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/slow.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(0);
        assertThat(result.getTotalFailures()).isEqualTo(1);
        assertThat(result.failedUrls().get(0)).contains("timed out");
    }

    @Test
    @Timeout(30)
    @DisplayName("E2E: Should handle 404 errors gracefully with improved structured concurrency")
    void should_handle404ErrorsGracefully_when_usingImprovedStructuredConcurrency() {
        // Given - Setup 404 response
        stubFor(get(urlEqualTo("/notfound.html"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Not Found</title></head><body>404 Error</body></html>")));

        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .maxDepth(1)
            .maxPages(5)
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("localhost")
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/notfound.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(0);
        assertThat(result.getTotalFailures()).isEqualTo(1);
        assertThat(result.failedUrls().get(0)).contains("/notfound.html");
    }

    @Test
    @Timeout(30)
    @DisplayName("E2E: Should demonstrate uniform cancellation when limits reached")
    void should_demonstrateUniformCancellation_when_limitsReached() {
        // Given - Setup mock responses with many links
        setupMockResponsesWithManyLinks();

        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .maxDepth(2)
            .maxPages(3) // Small limit to test cancellation
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("localhost")
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        // Should respect page limit (uniform cancellation working)
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(3);
        assertThat(result.getTotalFailures()).isGreaterThanOrEqualTo(0);

        // Should complete quickly due to early cancellation
        assertThat(result.getDurationMs()).isLessThan(10000);
    }

    @Test
    @Timeout(30)
    @DisplayName("E2E: Should handle mixed success and failure scenarios")
    void should_handleMixedSuccessAndFailureScenarios_when_usingImprovedStructuredConcurrency() {
        // Given - Setup mixed responses
        setupMixedMockResponses();

        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("localhost")
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isGreaterThanOrEqualTo(1);
        assertThat(result.getTotalFailures()).isGreaterThanOrEqualTo(0);
        assertThat(result.successfulPages().size()).isEqualTo(result.getTotalPagesCrawled());
        assertThat(result.failedUrls().size()).isEqualTo(result.getTotalFailures());
    }

    @Test
    @Timeout(30)
    @DisplayName("E2E: Should not follow external links when configured")
    void should_notFollowExternalLinks_when_configured() {
        // Given - Setup responses with external links
        setupMockResponsesWithExternalLinks();

        Crawler crawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(false) // Don't follow external links
            .startDomain("localhost")
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isGreaterThanOrEqualTo(1);
        assertThat(result.getTotalFailures()).isEqualTo(0);

        // Should not crawl external domains
        assertThat(result.successfulPages().stream()
            .noneMatch(page -> page.url().contains("external.com"))).isTrue();
    }

    private void setupMockResponses() {
        // Index page with links to other pages
        stubFor(get(urlEqualTo("/index.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Home</title></head><body>" +
                    "<a href=\"/about.html\">About</a>" +
                    "<a href=\"/contact.html\">Contact</a>" +
                    "</body></html>")));

        // About page
        stubFor(get(urlEqualTo("/about.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>About</title></head><body>" +
                    "<a href=\"/contact.html\">Contact</a>" +
                    "</body></html>")));

        // Contact page
        stubFor(get(urlEqualTo("/contact.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Contact</title></head><body>" +
                    "<a href=\"/index.html\">Home</a>" +
                    "</body></html>")));
    }

    private void setupMockResponsesWithManyLinks() {
        // Index page with many links
        StringBuilder body = new StringBuilder("<html><head><title>Home</title></head><body>");
        for (int i = 1; i <= 10; i++) {
            body.append("<a href=\"/page").append(i).append(".html\">Page ").append(i).append("</a>");
        }
        body.append("</body></html>");

        stubFor(get(urlEqualTo("/index.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(body.toString())));

        // Individual pages
        for (int i = 1; i <= 10; i++) {
            stubFor(get(urlEqualTo("/page" + i + ".html"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/html")
                    .withBody("<html><head><title>Page " + i + "</title></head><body>Content " + i + "</body></html>")));
        }
    }

    private void setupMixedMockResponses() {
        // Index page with mixed links
        stubFor(get(urlEqualTo("/index.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Home</title></head><body>" +
                    "<a href=\"/success.html\">Success</a>" +
                    "<a href=\"/error.html\">Error</a>" +
                    "</body></html>")));

        // Success page
        stubFor(get(urlEqualTo("/success.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Success</title></head><body>Success content</body></html>")));

        // Error page (500)
        stubFor(get(urlEqualTo("/error.html"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Error</title></head><body>Server Error</body></html>")));
    }

    private void setupMockResponsesWithExternalLinks() {
        // Index page with external links
        stubFor(get(urlEqualTo("/index.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Home</title></head><body>" +
                    "<a href=\"/internal.html\">Internal</a>" +
                    "<a href=\"http://external.com/page.html\">External</a>" +
                    "</body></html>")));

        // Internal page
        stubFor(get(urlEqualTo("/internal.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Internal</title></head><body>Internal content</body></html>")));
    }
}
