package info.jab.crawler.v13;

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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for StructuredQueueCrawler using WireMock to simulate a real website.
 *
 * This test simulates a small 3-page website:
 * - /index.html (links to /about and /contact)
 * - /about.html (links to /contact)
 * - /contact.html (no outgoing links)
 */
class StructuredQueueCrawlerIT {

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
    @DisplayName("Should crawl all 3 pages starting from index using multiple threads")
    void testCrawlAllPages() {
        // Given
        StructuredQueueCrawler crawler = (StructuredQueueCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.STRUCTURED_QUEUE_CRAWLER)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(3)
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

        // Note: We don't verify exact request counts in multi-threaded tests
        // due to potential race conditions in thread scheduling
    }

    @Test
    @DisplayName("Should respect maxPages limit")
    void testMaxPagesLimit() {
        // Given
        StructuredQueueCrawler crawler = (StructuredQueueCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.STRUCTURED_QUEUE_CRAWLER)
            .maxDepth(2)
            .maxPages(2)  // Limit to 2 pages
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should respect maxDepth limit")
    void testMaxDepthLimit() {
        // Given
        StructuredQueueCrawler crawler = (StructuredQueueCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.STRUCTURED_QUEUE_CRAWLER)
            .maxDepth(0)  // Only crawl the seed URL
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1);
        assertThat(result.successfulPages().get(0).url())
            .isEqualTo(baseUrl + "/index.html");
    }

    @Test
    @DisplayName("Should extract correct number of links from each page")
    void testLinkExtraction() {
        // Given
        StructuredQueueCrawler crawler = (StructuredQueueCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.STRUCTURED_QUEUE_CRAWLER)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
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
    @DisplayName("Should handle 404 errors gracefully")
    void testHandles404Errors() {
        // Given - add a broken link to index page
        stubFor(get(urlEqualTo("/index-with-404.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("index-with-404.html")
                .withTransformers("response-template")));

        stubFor(get(urlEqualTo("/broken.html"))
            .willReturn(aResponse().withStatus(404)));

        StructuredQueueCrawler crawler = (StructuredQueueCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.STRUCTURED_QUEUE_CRAWLER)
            .maxDepth(1)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index-with-404.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(1);
        assertThat(result.getTotalFailures()).isEqualTo(1);
        assertThat(result.failedUrls()).contains(baseUrl + "/broken.html");
    }

    @Test
    @DisplayName("Should avoid duplicate crawling of same URL")
    void testAvoidsDuplicates() {
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

        StructuredQueueCrawler crawler = (StructuredQueueCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.STRUCTURED_QUEUE_CRAWLER)
            .maxDepth(1)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/duplicates.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(2);

        // Note: We don't verify exact request counts in multi-threaded tests
        // The important check is that no duplicate pages appear in results
    }

    @Test
    @DisplayName("Should extract page content correctly")
    void testContentExtraction() {
        // Given
        StructuredQueueCrawler crawler = (StructuredQueueCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.STRUCTURED_QUEUE_CRAWLER)
            .maxDepth(0)
            .maxPages(1)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(1)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        Page page = result.successfulPages().get(0);
        assertThat(page.content())
            .contains("Welcome to Test Site")
            .contains("This is the home page");
    }

    @Test
    @DisplayName("Should work efficiently with different thread counts")
    void testDifferentThreadCounts() {
        // Given - test with 1, 2, and 4 threads
        int[] threadCounts = {1, 2, 4};

        for (int threads : threadCounts) {
            // Reset WireMock
            WireMock.reset();
            setupMockWebsite();

            StructuredQueueCrawler crawler = (StructuredQueueCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.STRUCTURED_QUEUE_CRAWLER)
                .maxDepth(2)
                .maxPages(10)
                .timeout(5000)
                .followExternalLinks(true)
                .numThreads(threads)
                .build();

            // When
            CrawlResult result = crawler.crawl(baseUrl + "/index.html");

            // Then
            assertThat(result.getTotalPagesCrawled())
                .as("Should crawl all pages with " + threads + " threads")
                .isEqualTo(3);
            assertThat(result.getTotalFailures()).isEqualTo(0);
        }
    }
}
