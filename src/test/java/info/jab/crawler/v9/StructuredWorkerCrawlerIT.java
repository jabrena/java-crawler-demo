package info.jab.crawler.v9;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for StructuredWorkerCrawler using WireMock.
 *
 * Tests the crawler's behavior with mocked HTTP responses to ensure:
 * - Proper HTTP request handling
 * - Link extraction and following
 * - Thread safety in multi-threaded environment
 * - Error handling for various HTTP status codes
 * - Performance characteristics with controlled responses
 */
class StructuredWorkerCrawlerIT {

    private WireMockServer wireMockServer;
    private StructuredWorkerCrawler crawler;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8080);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8080);
        baseUrl = "http://localhost:8080";

        crawler = new StructuredWorkerCrawler(
            2,      // maxDepth
            10,     // maxPages
            5000,   // timeoutMs
            false,  // followExternalLinks
            "localhost", // startDomain
            4       // numThreads
        );
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("Should crawl a simple page successfully")
    void shouldCrawlSimplePageSuccessfully() {
        // Given
        String html = """
            <html>
                <head><title>Test Page</title></head>
                <body>
                    <h1>Hello World</h1>
                    <p>This is a test page.</p>
                </body>
            </html>
            """;

        stubFor(get(urlEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(html)));

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1);
        assertThat(result.getTotalFailures()).isEqualTo(0);
        assertThat(result.successfulPages()).hasSize(1);

        Page page = result.successfulPages().get(0);
        assertThat(page.url()).isEqualTo(baseUrl + "/");
        assertThat(page.title()).isEqualTo("Test Page");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.content()).contains("Hello World");
        assertThat(page.links()).isEmpty();
    }

    @Test
    @DisplayName("Should follow links and crawl multiple pages")
    void shouldFollowLinksAndCrawlMultiplePages() {
        // Given
        String mainPageHtml = """
            <html>
                <head><title>Main Page</title></head>
                <body>
                    <h1>Main Page</h1>
                    <a href="/page1">Page 1</a>
                    <a href="/page2">Page 2</a>
                </body>
            </html>
            """;

        String page1Html = """
            <html>
                <head><title>Page 1</title></head>
                <body>
                    <h1>Page 1</h1>
                    <a href="/page3">Page 3</a>
                </body>
            </html>
            """;

        String page2Html = """
            <html>
                <head><title>Page 2</title></head>
                <body>
                    <h1>Page 2</h1>
                </body>
            </html>
            """;

        String page3Html = """
            <html>
                <head><title>Page 3</title></head>
                <body>
                    <h1>Page 3</h1>
                </body>
            </html>
            """;

        stubFor(get(urlEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(mainPageHtml)));

        stubFor(get(urlEqualTo("/page1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(page1Html)));

        stubFor(get(urlEqualTo("/page2"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(page2Html)));

        stubFor(get(urlEqualTo("/page3"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(page3Html)));

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(4);
        assertThat(result.getTotalFailures()).isEqualTo(0);
        assertThat(result.successfulPages()).hasSize(4);

        // Verify all pages were crawled
        assertThat(result.successfulPages().stream().map(Page::url))
            .containsExactlyInAnyOrder(
                baseUrl + "/",
                baseUrl + "/page1",
                baseUrl + "/page2",
                baseUrl + "/page3"
            );
    }

    @Test
    @DisplayName("Should respect maximum depth limit")
    void shouldRespectMaximumDepthLimit() {
        // Given
        StructuredWorkerCrawler shallowCrawler = new StructuredWorkerCrawler(
            1,      // maxDepth (only 1 level deep)
            10,     // maxPages
            5000,   // timeoutMs
            false,  // followExternalLinks
            "localhost", // startDomain
            4       // numThreads
        );

        String mainPageHtml = """
            <html>
                <head><title>Main Page</title></head>
                <body>
                    <a href="/page1">Page 1</a>
                    <a href="/page2">Page 2</a>
                </body>
            </html>
            """;

        String page1Html = """
            <html>
                <head><title>Page 1</title></head>
                <body>
                    <a href="/page3">Page 3 (should not be crawled)</a>
                </body>
            </html>
            """;

        stubFor(get(urlEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(mainPageHtml)));

        stubFor(get(urlEqualTo("/page1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(page1Html)));

        // When
        CrawlResult result = shallowCrawler.crawl(baseUrl + "/");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(2); // Main page + page1
        assertThat(result.successfulPages().stream().map(Page::url))
            .containsExactlyInAnyOrder(baseUrl + "/", baseUrl + "/page1");

        // Verify page3 was not crawled (beyond depth limit)
        verify(0, getRequestedFor(urlEqualTo("/page3")));
    }

    @Test
    @DisplayName("Should handle HTTP errors gracefully")
    void shouldHandleHttpErrorsGracefully() {
        // Given
        String mainPageHtml = """
            <html>
                <head><title>Main Page</title></head>
                <body>
                    <a href="/good-page">Good Page</a>
                    <a href="/bad-page">Bad Page</a>
                </body>
            </html>
            """;

        String goodPageHtml = """
            <html>
                <head><title>Good Page</title></head>
                <body>
                    <h1>Good Page</h1>
                </body>
            </html>
            """;

        stubFor(get(urlEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(mainPageHtml)));

        stubFor(get(urlEqualTo("/good-page"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(goodPageHtml)));

        stubFor(get(urlEqualTo("/bad-page"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "text/html")
                .withBody("Not Found")));

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(2); // Main page + good page
        assertThat(result.getTotalFailures()).isEqualTo(1); // Bad page failed
        assertThat(result.failedUrls()).contains(baseUrl + "/bad-page");
    }

    @Test
    @DisplayName("Should handle slow responses with timeout")
    void shouldHandleSlowResponsesWithTimeout() {
        // Given
        StructuredWorkerCrawler timeoutCrawler = new StructuredWorkerCrawler(
            1,      // maxDepth
            5,      // maxPages
            1000,   // timeoutMs (1 second)
            false,  // followExternalLinks
            "localhost", // startDomain
            2       // numThreads
        );

        String mainPageHtml = """
            <html>
                <head><title>Main Page</title></head>
                <body>
                    <a href="/slow-page">Slow Page</a>
                </body>
            </html>
            """;

        stubFor(get(urlEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(mainPageHtml)));

        stubFor(get(urlEqualTo("/slow-page"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><body>Slow page</body></html>")
                .withFixedDelay(2000))); // 2 second delay

        // When
        CrawlResult result = timeoutCrawler.crawl(baseUrl + "/");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1); // Only main page
        assertThat(result.getTotalFailures()).isEqualTo(1); // Slow page timed out
        assertThat(result.failedUrls()).contains(baseUrl + "/slow-page");
    }

    @Test
    @DisplayName("Should filter external links when configured")
    void shouldFilterExternalLinksWhenConfigured() {
        // Given
        String mainPageHtml = """
            <html>
                <head><title>Main Page</title></head>
                <body>
                    <a href="/internal">Internal Page</a>
                    <a href="http://external.com/page">External Page</a>
                </body>
            </html>
            """;

        String internalPageHtml = """
            <html>
                <head><title>Internal Page</title></head>
                <body>
                    <h1>Internal Page</h1>
                </body>
            </html>
            """;

        stubFor(get(urlEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(mainPageHtml)));

        stubFor(get(urlEqualTo("/internal"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(internalPageHtml)));

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(2); // Main page + internal page
        assertThat(result.successfulPages().stream().map(Page::url))
            .containsExactlyInAnyOrder(baseUrl + "/", baseUrl + "/internal");

        // Verify external page was not requested
        verify(0, getRequestedFor(urlEqualTo("http://external.com/page")));
    }

    @Test
    @DisplayName("Should handle concurrent requests safely")
    void shouldHandleConcurrentRequestsSafely() {
        // Given
        String mainPageHtml = """
            <html>
                <head><title>Main Page</title></head>
                <body>
                    <a href="/page1">Page 1</a>
                    <a href="/page2">Page 2</a>
                    <a href="/page3">Page 3</a>
                    <a href="/page4">Page 4</a>
                </body>
            </html>
            """;

        stubFor(get(urlEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody(mainPageHtml)));

        // Stub all pages with simple responses
        for (int i = 1; i <= 4; i++) {
            String pageHtml = String.format("""
                <html>
                    <head><title>Page %d</title></head>
                    <body>
                        <h1>Page %d</h1>
                    </body>
                </html>
                """, i, i);

            stubFor(get(urlEqualTo("/page" + i))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/html")
                    .withBody(pageHtml)));
        }

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPagesCrawled()).isEqualTo(5); // Main page + 4 sub-pages
        assertThat(result.getTotalFailures()).isEqualTo(0);

        // Verify all pages were requested (concurrent execution)
        verify(getRequestedFor(urlEqualTo("/")));
        verify(getRequestedFor(urlEqualTo("/page1")));
        verify(getRequestedFor(urlEqualTo("/page2")));
        verify(getRequestedFor(urlEqualTo("/page3")));
        verify(getRequestedFor(urlEqualTo("/page4")));
    }
}
