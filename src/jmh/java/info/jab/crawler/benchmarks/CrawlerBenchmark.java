package info.jab.crawler.benchmarks;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlerType;
import info.jab.crawler.commons.CrawlResult;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * JMH benchmark for crawler implementations performance testing.
 * This benchmark tests all 8 crawler implementations against the Cursor Rules for Java website
 * to measure and compare their performance characteristics.
 *
 * Test URL: https://jabrena.github.io/cursor-rules-java/
 *
 * Crawler implementations tested:
 * 1. Sequential Crawler (v1)
 * 2. Producer-Consumer Crawler (v2)
 * 3. Recursive Crawler (v3)
 * 4. Multi-threaded Recursive Crawler (v4)
 * 5. Actor Crawler (v5)
 * 6. Recursive Actor Crawler (v6)
 * 7. Structural Concurrency Crawler (v7)
 * 8. Hybrid Actor-Structural Crawler (v8)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms1G", "-Xmx2G", "--enable-preview"})
@Warmup(iterations = 2)
@Measurement(iterations = 3)
public class CrawlerBenchmark {

    // Test URL: Cursor Rules for Java website
    private static final String TEST_URL = "https://jabrena.github.io/cursor-rules-java/";

    // Benchmark configuration for consistent testing
    private static final int MAX_DEPTH = 2;
    private static final int MAX_PAGES = 10;
    private static final int TIMEOUT_MS = 5000;
    private static final boolean FOLLOW_EXTERNAL_LINKS = false;
    private static final int NUM_THREADS = 4;

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
        return crawler.crawl(TEST_URL);
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
        return crawler.crawl(TEST_URL);
    }

    @Benchmark
    public CrawlResult benchmarkRecursiveCrawler() {
        Crawler crawler = CrawlerType.RECURSIVE.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
        );
        return crawler.crawl(TEST_URL);
    }

    @Benchmark
    public CrawlResult benchmarkMultiThreadedRecursiveCrawler() {
        Crawler crawler = CrawlerType.MULTI_THREADED_RECURSIVE.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
                   .numThreads(NUM_THREADS)
        );
        return crawler.crawl(TEST_URL);
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
        return crawler.crawl(TEST_URL);
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
        return crawler.crawl(TEST_URL);
    }

    @Benchmark
    public CrawlResult benchmarkStructuralConcurrencyCrawler() {
        Crawler crawler = CrawlerType.STRUCTURAL_CONCURRENCY.createWith(builder ->
            builder.maxDepth(MAX_DEPTH)
                   .maxPages(MAX_PAGES)
                   .timeout(TIMEOUT_MS)
                   .followExternalLinks(FOLLOW_EXTERNAL_LINKS)
        );
        return crawler.crawl(TEST_URL);
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
        return crawler.crawl(TEST_URL);
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

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
                type == CrawlerType.MULTI_THREADED_RECURSIVE ||
                type == CrawlerType.ACTOR ||
                type == CrawlerType.RECURSIVE_ACTOR ||
                type == CrawlerType.HYBRID_ACTOR_STRUCTURAL) {
                builder.numThreads(NUM_THREADS);
            }
        });
    }

    /**
     * Main method to run benchmarks with JSON output configuration
     * Tests all crawler implementations against the Cursor Rules for Java website
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(CrawlerBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("src/jmh/test/resources/jmh-crawler-benchmark-results.json")
                .build();

        System.out.println("🚀 Starting JMH benchmarks for all crawler implementations");
        System.out.println("📊 Test URL: " + TEST_URL);
        System.out.println("⚙️  Configuration: maxDepth=" + MAX_DEPTH + ", maxPages=" + MAX_PAGES + ", timeout=" + TIMEOUT_MS + "ms");
        System.out.println("🧵 Threads: " + NUM_THREADS + " (for multi-threaded crawlers)");
        System.out.println("📈 Results will be saved to: src/jmh/test/resources/jmh-crawler-benchmark-results.json");
        System.out.println();

        new Runner(options).run();
    }
}
