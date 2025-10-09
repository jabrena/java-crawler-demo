package info.jab.crawler.v10;

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
import org.junit.jupiter.api.Timeout;

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
 * Integration tests for VirtualThreadActorCrawler using WireMock to simulate a real website.
 *
 * This test simulates a small 3-page website:
 * - /index.html (links to /about and /contact)
 * - /about.html (links to /contact)
 * - /contact.html (no outgoing links)
 */
class VirtualThreadActorCrawlerIT {

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
    @Timeout(30)
    @DisplayName("Should crawl all 3 pages starting from index")
    void testCrawlAllPages() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
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
    @Timeout(30)
    @DisplayName("Should respect maxPages limit")
    void testMaxPagesLimit() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(2)
            .maxPages(2)  // Limit to 2 pages
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
            .build();

        // When
        CrawlResult result = crawler.crawl(baseUrl + "/index.html");

        // Then
        assertThat(result.getTotalPagesCrawled()).isEqualTo(2);
    }

    @Test
    @Timeout(30)
    @DisplayName("Should respect maxDepth limit")
    void testMaxDepthLimit() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
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
    @Timeout(30)
    @DisplayName("Should extract correct number of links from each page")
    void testLinkExtraction() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
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
    @Timeout(30)
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

        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
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
    @Timeout(30)
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

        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
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

        // Verify target was only requested once
        verify(exactly(1), getRequestedFor(urlEqualTo("/target.html")));
    }

    @Test
    @Timeout(30)
    @DisplayName("Should extract page content correctly")
    void testContentExtraction() {
        // Given
        VirtualThreadActorCrawler crawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(0)
            .maxPages(1)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(2)
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
    @Timeout(30)
    @DisplayName("Should work with different number of actors")
    void testDifferentActorCounts() {
        // Given - Test with 1 actor
        VirtualThreadActorCrawler singleActorCrawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(1)
            .maxPages(3)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(1)
            .build();

        // Given - Test with 4 actors
        VirtualThreadActorCrawler multiActorCrawler = (VirtualThreadActorCrawler) new DefaultCrawlerBuilder().crawlerType(CrawlerType.VIRTUAL_THREAD_ACTOR)
            .maxDepth(1)
            .maxPages(3)
            .timeout(5000)
            .followExternalLinks(true)
            .numThreads(4)
            .build();

        // When
        CrawlResult singleResult = singleActorCrawler.crawl(baseUrl + "/index.html");
        CrawlResult multiResult = multiActorCrawler.crawl(baseUrl + "/index.html");

        // Then - Both should crawl the same pages
        assertThat(singleResult.getTotalPagesCrawled()).isEqualTo(multiResult.getTotalPagesCrawled());
        assertThat(singleResult.successfulPages())
            .extracting(Page::url)
            .containsExactlyInAnyOrderElementsOf(
                multiResult.successfulPages().stream().map(Page::url).toList()
            );
    }
}
