package info.jab.crawler.v6;

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
 * Integration tests for RecursiveActorCrawler using WireMock to simulate a real website.
 *
 * This test simulates a small 3-page website:
 * - /index.html (links to /about and /contact)
 * - /about.html (links to /contact)
 * - /contact.html (no outgoing links)
 */
class RecursiveActorCrawlerIT {

    private WireMockServer wireMockServer;
    private RecursiveActorCrawler crawler;

    @BeforeEach
    void setUp() {
        // Start WireMock server on a random port
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Create crawler pointing to WireMock server
        crawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("localhost")
            .numThreads(4)  // Max actors
            .build();

        // Setup mock responses
        setupMockResponses();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private void setupMockResponses() {
        // Mock index page with links to about and contact
        stubFor(get(urlEqualTo("/index.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("""
                    <html>
                        <head><title>Home Page</title></head>
                        <body>
                            <h1>Welcome to our site</h1>
                            <p>This is the home page.</p>
                            <a href="/about.html">About Us</a>
                            <a href="/contact.html">Contact</a>
                        </body>
                    </html>
                    """)));

        // Mock about page with link to contact
        stubFor(get(urlEqualTo("/about.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("""
                    <html>
                        <head><title>About Us</title></head>
                        <body>
                            <h1>About Us</h1>
                            <p>We are a great company.</p>
                            <a href="/contact.html">Contact Us</a>
                        </body>
                    </html>
                    """)));

        // Mock contact page with no outgoing links
        stubFor(get(urlEqualTo("/contact.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("""
                    <html>
                        <head><title>Contact</title></head>
                        <body>
                            <h1>Contact Us</h1>
                            <p>Get in touch with us.</p>
                        </body>
                    </html>
                    """)));

        // Mock a 404 page
        stubFor(get(urlEqualTo("/404.html"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "text/html")
                .withBody("""
                    <html>
                        <head><title>Not Found</title></head>
                        <body>
                            <h1>404 - Page Not Found</h1>
                        </body>
                    </html>
                    """)));
    }

    @Test
    @DisplayName("Should crawl all pages in the mock website using recursive actors")
    void shouldCrawlAllPagesInMockWebsite() {
        // Given
        String seedUrl = "http://localhost:" + wireMockServer.port() + "/index.html";

        // When
        CrawlResult result = crawler.crawl(seedUrl);

        // Then
        assertThat(result.successfulPages()).hasSize(3);
        assertThat(result.failedUrls()).isEmpty();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3);
        assertThat(result.getTotalFailures()).isEqualTo(0);

        // Verify all pages were crawled
        assertThat(result.successfulPages())
            .extracting(Page::url)
            .containsExactlyInAnyOrder(
                "http://localhost:" + wireMockServer.port() + "/index.html",
                "http://localhost:" + wireMockServer.port() + "/about.html",
                "http://localhost:" + wireMockServer.port() + "/contact.html"
            );

        // Verify page titles
        assertThat(result.successfulPages())
            .extracting(Page::title)
            .containsExactlyInAnyOrder("Home Page", "About Us", "Contact");

        // Verify HTTP requests were made
        verify(exactly(1), getRequestedFor(urlEqualTo("/index.html")));
        verify(exactly(1), getRequestedFor(urlEqualTo("/about.html")));
        verify(exactly(1), getRequestedFor(urlEqualTo("/contact.html")));
    }

    @Test
    @DisplayName("Should respect depth limit using recursive actors")
    void shouldRespectDepthLimit() {
        // Given - crawler with depth limit of 1
        RecursiveActorCrawler limitedCrawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(1)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("localhost")
            .numThreads(4)
            .build();

        String seedUrl = "http://localhost:" + wireMockServer.port() + "/index.html";

        // When
        CrawlResult result = limitedCrawler.crawl(seedUrl);

        // Then - should only crawl index page (depth 0) and direct links (depth 1)
        assertThat(result.successfulPages()).hasSize(3); // index, about, contact
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3);

        // Verify HTTP requests were made
        verify(exactly(1), getRequestedFor(urlEqualTo("/index.html")));
        verify(exactly(1), getRequestedFor(urlEqualTo("/about.html")));
        verify(exactly(1), getRequestedFor(urlEqualTo("/contact.html")));
    }

    @Test
    @DisplayName("Should respect page limit using recursive actors")
    void shouldRespectPageLimit() {
        // Given - crawler with page limit of 2
        RecursiveActorCrawler limitedCrawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(2)
            .maxPages(2)
            .timeout(5000)
            .followExternalLinks(false)
            .startDomain("localhost")
            .numThreads(4)
            .build();

        String seedUrl = "http://localhost:" + wireMockServer.port() + "/index.html";

        // When
        CrawlResult result = limitedCrawler.crawl(seedUrl);

        // Then - should crawl at most 2 pages
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(2);
        assertThat(result.successfulPages()).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should handle failed URLs gracefully using recursive actors")
    void shouldHandleFailedUrlsGracefully() {
        // Given
        String seedUrl = "http://localhost:" + wireMockServer.port() + "/404.html";

        // When
        CrawlResult result = crawler.crawl(seedUrl);

        // Then
        assertThat(result.successfulPages()).isEmpty();
        assertThat(result.failedUrls()).hasSize(1);
        assertThat(result.getTotalPagesCrawled()).isEqualTo(0);
        assertThat(result.getTotalFailures()).isEqualTo(1);
        assertThat(result.failedUrls()).contains(seedUrl);

        // Verify HTTP request was made
        verify(exactly(1), getRequestedFor(urlEqualTo("/404.html")));
    }

    @Test
    @DisplayName("Should not follow external links using recursive actors")
    void shouldNotFollowExternalLinks() {
        // Given - setup external link in index page
        stubFor(get(urlEqualTo("/external.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("""
                    <html>
                        <head><title>External Page</title></head>
                        <body>
                            <h1>External Page</h1>
                            <a href="https://external.com/page">External Link</a>
                        </body>
                    </html>
                    """)));

        String seedUrl = "http://localhost:" + wireMockServer.port() + "/external.html";

        // When
        CrawlResult result = crawler.crawl(seedUrl);

        // Then - should only crawl the external.html page, not follow external links
        assertThat(result.successfulPages()).hasSize(1);
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1);
        assertThat(result.successfulPages().get(0).url()).isEqualTo(seedUrl);

        // Verify only the external.html page was requested
        verify(exactly(1), getRequestedFor(urlEqualTo("/external.html")));
    }

    @Test
    @DisplayName("Should handle timeout gracefully using recursive actors")
    void shouldHandleTimeoutGracefully() {
        // Given - crawler with very short timeout
        RecursiveActorCrawler timeoutCrawler = (RecursiveActorCrawler) new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE_ACTOR)
            .maxDepth(2)
            .maxPages(10)
            .timeout(1) // 1ms timeout - very short
            .followExternalLinks(false)
            .startDomain("localhost")
            .numThreads(4)
            .build();

        String seedUrl = "http://localhost:" + wireMockServer.port() + "/index.html";

        // When
        CrawlResult result = timeoutCrawler.crawl(seedUrl);

        // Then - should handle timeout gracefully
        assertThat(result).isNotNull();
        // The exact behavior depends on timing, but it should not crash
    }

    @Test
    @DisplayName("Should complete within reasonable time using recursive actors")
    void shouldCompleteWithinReasonableTime() {
        // Given
        String seedUrl = "http://localhost:" + wireMockServer.port() + "/index.html";

        // When
        long startTime = System.currentTimeMillis();
        CrawlResult result = crawler.crawl(seedUrl);
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(10000); // Should complete within 10 seconds
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3);
    }
}
