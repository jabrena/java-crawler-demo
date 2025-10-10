package info.jab.crawler.benchmarks;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.commons.CrawlResult;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * JMH benchmark for crawler implementations performance testing.
 * This benchmark supports two testing modes:
 *
 * 1. WireMock Mode (default): Tests all crawler implementations using WireMock to simulate
 *    realistic HTTP responses with artificial latency based on previous real-world measurements.
 *    - Simulates a comprehensive 2-level depth website with 15 pages total
 *    - Website structure: 1 root page + 5 main sections + 9 sub-pages + 1 broken link (404)
 *    - Uses realistic latency values derived from previous benchmark results
 *    - Provides consistent, reproducible performance measurements
 *    - Tests error handling with a 404 response
 *
 * 2. Real Website Mode: Tests crawler implementations against the actual
 *    https://jabrena.github.io/cursor-rules-java/ website for real-world performance evaluation.
 *    - Tests against live website with real network latency
 *    - Provides actual performance measurements under real conditions
 *    - Tests real-world error handling and edge cases
 *
 * Website Structure (WireMock):
 * Level 0: index.html (entry point with links to all main sections)
 * Level 1: about.html, services.html, products.html, contact.html, blog.html
 * Level 2: 9 sub-pages (team, history, mission, web-dev, consulting, software, hardware, latest, archive)
 * Error: broken-link.html (404 response for error handling testing)
 *
 * Crawler implementations tested:
 * 1. Sequential Crawler (v1)
 * 2. Producer-Consumer Crawler (v2)
 * 3. Recursive Crawler (v3)
 * 4. Multi-threaded Iterative Crawler (v4)
 * 5. Actor Crawler (v5)
 * 6. Recursive Actor Crawler (v6)
 * 7. Structural Concurrency Crawler (v7)
 * 8. Hybrid Actor-Structural Crawler (v8)
 * 9. Structured Worker Crawler (v9)
 * 10. Virtual Thread Actor Crawler (v10)
 * 11. Improved Structured Concurrency Crawler (v11)
 * 12. Jox-based Structured Concurrency Crawler (v12)
 * 13. Structured Queue Crawler (v13)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms1G", "-Xmx2G", "--enable-preview"})
@Warmup(iterations = 2)
@Measurement(iterations = 5)
public class CrawlerBenchmark {

    // WireMock server and test URLs
    private WireMockServer wireMockServer;
    private String mockTestUrl;
    private String realTestUrl;

    // Real website URL for testing
    private static final String REAL_WEBSITE_URL = "https://jabrena.github.io/cursor-rules-java/";

    // Benchmark mode selection via system property
    // Use -Dbenchmark.mode=mock for WireMock testing or -Dbenchmark.mode=real for real website testing
    private static final String BENCHMARK_MODE = System.getProperty("benchmark.mode", "mock");
    private static final boolean USE_REAL_WEBSITE = BENCHMARK_MODE.equals("real");

    // Benchmark configuration for consistent testing
    private static final int MAX_DEPTH = 2;
    private static final int MAX_PAGES = 15;
    private static final int TIMEOUT_MS = 10000; // Increased timeout for real website
    private static final boolean FOLLOW_EXTERNAL_LINKS = false;
    private static final int NUM_THREADS = 4;

    // Realistic latency values derived from previous benchmark results
    // Based on analysis of 20251009-1511-jmh-results.json
    // These represent typical HTTP response times for a real website
    private static final int FAST_LATENCY_MS = 40;    // Fast pages: ~35-45ms
    private static final int MEDIUM_LATENCY_MS = 60;  // Medium pages: ~55-65ms
    private static final int SLOW_LATENCY_MS = 80;    // Slow pages: ~75-85ms
    private static final int VERY_SLOW_LATENCY_MS = 120; // Very slow pages: ~110-130ms

    @Setup(Level.Trial)
    public void setup() {
        if (USE_REAL_WEBSITE) {
            // For real website testing, no WireMock setup needed
            realTestUrl = REAL_WEBSITE_URL;
        } else {
            // Start WireMock server on random port for mock testing
            wireMockServer = new WireMockServer(
                WireMockConfiguration.options()
                    .dynamicPort()
                    .withRootDirectory("src/test/resources")
            );
            wireMockServer.start();
            WireMock.configureFor("localhost", wireMockServer.port());

            // Initialize mock test URL
            mockTestUrl = "http://localhost:" + wireMockServer.port() + "/index.html";

            // Set up mock website with realistic latency
            setupMockWebsite();
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    /**
     * Sets up a comprehensive mock website with 2 levels of depth and 15 pages total.
     * Website structure:
     * Level 0: index.html (entry point)
     * Level 1: about.html, services.html, products.html, contact.html, blog.html
     * Level 2: Multiple sub-pages under each level 1 category
     * Plus one broken link (404) to test error handling
     */
    private void setupMockWebsite() {
        // ============================================================================
        // LEVEL 0: ROOT PAGE
        // ============================================================================

        // Home page - entry point with links to all main sections
        stubFor(get(urlEqualTo("/index.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("index.html")
                .withFixedDelay(FAST_LATENCY_MS)
                .withTransformers("response-template")));

        // ============================================================================
        // LEVEL 1: MAIN SECTIONS
        // ============================================================================

        // About page with links to sub-pages
        stubFor(get(urlEqualTo("/about.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("about.html")
                .withFixedDelay(MEDIUM_LATENCY_MS)
                .withTransformers("response-template")));

        // Services page
        stubFor(get(urlEqualTo("/services.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("services.html")
                .withFixedDelay(SLOW_LATENCY_MS)
                .withTransformers("response-template")));

        // Products page
        stubFor(get(urlEqualTo("/products.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("products.html")
                .withFixedDelay(MEDIUM_LATENCY_MS)
                .withTransformers("response-template")));

        // Contact page
        stubFor(get(urlEqualTo("/contact.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("contact.html")
                .withFixedDelay(FAST_LATENCY_MS)));

        // Blog page
        stubFor(get(urlEqualTo("/blog.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("blog.html")
                .withFixedDelay(VERY_SLOW_LATENCY_MS)
                .withTransformers("response-template")));

        // ============================================================================
        // LEVEL 2: SUB-PAGES
        // ============================================================================

        // About sub-pages
        stubFor(get(urlEqualTo("/about/team.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("about-team.html")
                .withFixedDelay(MEDIUM_LATENCY_MS)));

        stubFor(get(urlEqualTo("/about/history.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("about-history.html")
                .withFixedDelay(SLOW_LATENCY_MS)));

        stubFor(get(urlEqualTo("/about/mission.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("about-mission.html")
                .withFixedDelay(FAST_LATENCY_MS)));

        // Services sub-pages
        stubFor(get(urlEqualTo("/services/web-development.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("services-web-development.html")
                .withFixedDelay(VERY_SLOW_LATENCY_MS)));

        stubFor(get(urlEqualTo("/services/consulting.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("services-consulting.html")
                .withFixedDelay(MEDIUM_LATENCY_MS)));

        // Products sub-pages
        stubFor(get(urlEqualTo("/products/software.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("products-software.html")
                .withFixedDelay(SLOW_LATENCY_MS)));

        stubFor(get(urlEqualTo("/products/hardware.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("products-hardware.html")
                .withFixedDelay(MEDIUM_LATENCY_MS)));

        // Blog sub-pages
        stubFor(get(urlEqualTo("/blog/latest.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("blog-latest.html")
                .withFixedDelay(FAST_LATENCY_MS)));

        stubFor(get(urlEqualTo("/blog/archive.html"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("blog-archive.html")
                .withFixedDelay(SLOW_LATENCY_MS)));

        // ============================================================================
        // BROKEN LINK (404 ERROR) - Tests error handling
        // ============================================================================

        // This page is linked from some pages but returns 404
        stubFor(get(urlEqualTo("/broken-link.html"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "text/html")
                .withBodyFile("broken-link.html")
                .withFixedDelay(FAST_LATENCY_MS)));

        // Note: The about.html page already includes the broken link in the HTML file
    }

    // ============================================================================
    // CRAWLER IMPLEMENTATION BENCHMARKS
    // ============================================================================

    @Benchmark
    public CrawlResult benchmarkSequentialCrawler() {
        Crawler crawler = CrawlerType.SEQUENTIAL.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkProducerConsumerCrawler() {
        Crawler crawler = CrawlerType.PRODUCER_CONSUMER.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
                   .numThreads(NUM_THREADS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkRecursiveCrawler() {
        Crawler crawler = CrawlerType.RECURSIVE.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkMultiThreadedIterativeCrawler() {
        Crawler crawler = CrawlerType.MULTI_THREADED_ITERATIVE.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
                   .numThreads(NUM_THREADS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkActorCrawler() {
        Crawler crawler = CrawlerType.ACTOR.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
                   .numThreads(NUM_THREADS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkRecursiveActorCrawler() {
        Crawler crawler = CrawlerType.RECURSIVE_ACTOR.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
                   .numThreads(NUM_THREADS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkStructuralConcurrencyCrawler() {
        Crawler crawler = CrawlerType.STRUCTURAL_CONCURRENCY.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkHybridActorStructuralCrawler() {
        Crawler crawler = CrawlerType.HYBRID_ACTOR_STRUCTURAL.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
                   .numThreads(NUM_THREADS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkStructuredWorkerCrawler() {
        Crawler crawler = CrawlerType.STRUCTURED_WORKER.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
                   .numThreads(NUM_THREADS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkVirtualThreadActorCrawler() {
        Crawler crawler = CrawlerType.VIRTUAL_THREAD_ACTOR.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
                   .numThreads(NUM_THREADS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkImprovedStructuredConcurrencyCrawler() {
        Crawler crawler = CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkJoxCrawler() {
        Crawler crawler = CrawlerType.JOX_STRUCTURED_CONCURRENCY.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
        );
        return crawler.crawl(getTestUrl());
    }

    @Benchmark
    public CrawlResult benchmarkStructuredQueueCrawler() {
        Crawler crawler = CrawlerType.STRUCTURED_QUEUE_CRAWLER.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
                   .numThreads(NUM_THREADS)
        );
        return crawler.crawl(getTestUrl());
    }


    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Gets the appropriate test URL based on the benchmark mode
     */
    private String getTestUrl() {
        return USE_REAL_WEBSITE ? realTestUrl : mockTestUrl;
    }

    /**
     * Helper method to create a crawler with consistent configuration
     */
    private Crawler createCrawler(CrawlerType type) {
        return type.createWith(builder -> {
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS);

            // Add thread configuration for multi-threaded crawlers
            if (type == CrawlerType.PRODUCER_CONSUMER ||
                type == CrawlerType.MULTI_THREADED_ITERATIVE ||
                type == CrawlerType.ACTOR ||
                type == CrawlerType.RECURSIVE_ACTOR ||
                type == CrawlerType.HYBRID_ACTOR_STRUCTURAL ||
                type == CrawlerType.STRUCTURED_WORKER ||
                type == CrawlerType.VIRTUAL_THREAD_ACTOR ||
                type == CrawlerType.STRUCTURED_QUEUE_CRAWLER) {
                builder.numThreads(NUM_THREADS);
            }
        });
    }

    /**
     * Main method to run benchmarks with JSON output configuration and JFR profiling
     * Tests all crawler implementations using either WireMock simulation or real website testing
     *
     * Usage:
     * - Default (mock): mvn exec:java -Pjmh -Dexec.mainClass="info.jab.crawler.benchmarks.CrawlerBenchmark"
     * - Real website: mvn exec:java -Pjmh -Dexec.mainClass="info.jab.crawler.benchmarks.CrawlerBenchmark" -Dbenchmark.mode=real
     * - Mock website: mvn exec:java -Pjmh -Dexec.mainClass="info.jab.crawler.benchmarks.CrawlerBenchmark" -Dbenchmark.mode=mock
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(CrawlerBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("src/jmh/test/resources/jmh-crawler-benchmark-results.json")
                .addProfiler("jfr", "dir=src/jmh/test/resources")  // Enable JFR profiling with output directory
                .build();

        System.out.println("🚀 Starting JMH benchmarks for all crawler implementations");
        System.out.println("📊 Benchmark Mode: " + BENCHMARK_MODE.toUpperCase());

        if (USE_REAL_WEBSITE) {
            System.out.println("🌐 Real Website Configuration:");
            System.out.println("   • Target: " + REAL_WEBSITE_URL);
            System.out.println("   • Real network latency and conditions");
            System.out.println("   • Live error handling and edge cases");
        } else {
            System.out.println("🔧 WireMock Configuration:");
            System.out.println("   • Mock website: 2-level depth, 15 pages total + 1 broken link (404)");
            System.out.println("   • Website structure:");
            System.out.println("     - Level 0: index.html (entry point)");
            System.out.println("     - Level 1: about, services, products, contact, blog");
            System.out.println("     - Level 2: 9 sub-pages across different sections");
            System.out.println("     - Error page: broken-link.html (404 response)");
            System.out.println("   • Latency simulation: " + FAST_LATENCY_MS + "ms, " + MEDIUM_LATENCY_MS + "ms, " + SLOW_LATENCY_MS + "ms, " + VERY_SLOW_LATENCY_MS + "ms");
        }

        System.out.println();
        System.out.println("⚙️  Common Configuration:");
        System.out.println("   • maxDepth=" + MAX_DEPTH + ", maxPages=" + MAX_PAGES + ", timeout=" + TIMEOUT_MS + "ms");
        System.out.println("   • Threads: " + NUM_THREADS + " (for multi-threaded crawlers)");
        System.out.println("📈 Results will be saved to: src/jmh/test/resources/jmh-crawler-benchmark-results.json");
        System.out.println("🔍 JFR profiling enabled - flight recorder files will be generated in src/jmh/test/resources/");
        System.out.println("📁 JFR files will be named: <benchmark-name>.jfr");
        System.out.println();

        new Runner(options).run();
    }
}
