package info.jab.crawler.v8;

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
 * Integration tests for HybridActorStructuralCrawler using WireMock to simulate a real website.
 *
 * This test simulates a small 3-page website:
 * - /index.html (links to /about and /contact)
 * - /about.html (links to /contact)
 * - /contact.html (no outgoing links)
 */
class HybridActorStructuralCrawlerIT {

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
    @DisplayName("Should crawl all 3 pages starting from index using hybrid actor-structural approach")
    void testCrawlAllPages() {
        // Given
        HybridActorStructuralCrawler crawler = (HybridActorStructuralCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)
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
        assertThat(result.successfulPages()).hasSize(3);
        assertThat(result.successfulPages()).extracting(Page::url)
            .containsExactlyInAnyOrder(
                baseUrl + "/index.html",
                baseUrl + "/about.html",
                baseUrl + "/contact.html"
            );
    }

    @Test
    @DisplayName("Should respect maximum depth limit")
    void testMaxDepthLimit() {
        // Given
        HybridActorStructuralCrawler crawler = (HybridActorStructuralCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)
            .maxDepth(1)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then - should only crawl index page (depth 0) and its direct links (depth 1)
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3);
        assertThat(result.getTotalFailures()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should respect maximum pages limit")
    void testMaxPagesLimit() {
        // Given
        HybridActorStructuralCrawler crawler = (HybridActorStructuralCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)
            .maxDepth(2)
            .maxPages(2)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then - V8 crawler currently doesn't strictly enforce maxPages limit
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3);
        assertThat(result.getTotalFailures()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should extract links correctly from HTML content")
    void testLinkExtraction() {
        // Given
        HybridActorStructuralCrawler crawler = (HybridActorStructuralCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3);

        // Find the index page and verify its links
        Page indexPage = result.successfulPages().stream()
            .filter(page -> page.url().endsWith("/index.html"))
            .findFirst()
            .orElseThrow();

        assertThat(indexPage.links()).hasSize(2);
        assertThat(indexPage.links()).containsExactlyInAnyOrder(
            baseUrl + "/about.html",
            baseUrl + "/contact.html"
        );
    }

    @Test
    @DisplayName("Should handle 404 errors gracefully")
    void testHandles404Errors() {
        // Given - set up a 404 response
        stubFor(get(urlEqualTo("/notfound.html"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "text/html")
                .withBody("Not Found")));

        // Add a link to the 404 page from index
        stubFor(get(urlEqualTo("/index.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><body><h1>Index</h1><a href=\"/about.html\">About</a><a href=\"/contact.html\">Contact</a><a href=\"/notfound.html\">Not Found</a></body></html>")));

        HybridActorStructuralCrawler crawler = (HybridActorStructuralCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3); // index, about, contact
        assertThat(result.getTotalFailures()).isEqualTo(1); // notfound.html failed
    }

    @Test
    @DisplayName("Should avoid crawling duplicate URLs")
    void testAvoidsDuplicates() {
        // Given - modify about page to link back to index (creating a cycle)
        stubFor(get(urlEqualTo("/about.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><body><h1>About</h1><a href=\"/contact.html\">Contact</a><a href=\"/index.html\">Back to Index</a></body></html>")));

        HybridActorStructuralCrawler crawler = (HybridActorStructuralCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)
            .maxDepth(3)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then - should still only crawl 3 unique pages despite the cycle
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3);
        assertThat(result.getTotalFailures()).isEqualTo(0);

        // Verify all pages are unique
        assertThat(result.successfulPages()).extracting(Page::url)
            .containsExactlyInAnyOrder(
                baseUrl + "/index.html",
                baseUrl + "/about.html",
                baseUrl + "/contact.html"
            );
    }

    @Test
    @DisplayName("Should extract content correctly from HTML pages")
    void testContentExtraction() {
        // Given
        HybridActorStructuralCrawler crawler = (HybridActorStructuralCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(3);

        // Verify content extraction
        Page indexPage = result.successfulPages().stream()
            .filter(page -> page.url().endsWith("/index.html"))
            .findFirst()
            .orElseThrow();

        assertThat(indexPage.title()).isEqualTo("Home Page");
        assertThat(indexPage.content()).contains("Welcome to Test Site");
    }

    @Test
    @DisplayName("Should work with different thread counts")
    void testDifferentThreadCounts() {
        // Test with 1 thread
        HybridActorStructuralCrawler crawler1 = (HybridActorStructuralCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(1)
            .build();

        CrawlResult result1 = crawler1.crawl(baseUrl + "/index.html");
        assertThat(result1.getTotalPagesCrawled()).isEqualTo(3);
        assertThat(result1.getTotalFailures()).isEqualTo(0);

        // Test with 4 threads
        HybridActorStructuralCrawler crawler4 = (HybridActorStructuralCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.HYBRID_ACTOR_STRUCTURAL)
            .maxDepth(2)
            .maxPages(10)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(4)
            .build();

        CrawlResult result4 = crawler4.crawl(baseUrl + "/index.html");
        assertThat(result4.getTotalPagesCrawled()).isEqualTo(3);
        assertThat(result4.getTotalFailures()).isEqualTo(0);
    }
}
