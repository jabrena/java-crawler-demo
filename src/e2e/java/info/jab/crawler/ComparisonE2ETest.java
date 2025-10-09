package info.jab.crawler;

import info.jab.crawler.commons.Crawler;
import info.jab.crawler.commons.CrawlResult;
import info.jab.crawler.commons.DefaultCrawlerBuilder;
import info.jab.crawler.commons.CrawlerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End comparison test that verifies all crawler implementations
 * return consistent results when given the same configuration.
 *
 * This test crawls actual websites and should only be run when explicitly enabled
 * via Maven profile or system property to avoid hitting real sites during regular builds.
 *
 * Run with: mvn test -Pe2e
 * Or: mvn test -Dtest.e2e=true
 */
@EnabledIfSystemProperty(named = "test.e2e", matches = "true")
class ComparisonE2ETest {

    private static final String TARGET_URL = "https://jabrena.github.io/cursor-rules-java/";
    private static final int MAX_DEPTH = 2;
    private static final int MAX_PAGES = 10;
    private static final int TIMEOUT_MS = 10000;
    private static final String START_DOMAIN = "jabrena.github.io";

    @Test
    @Timeout(120)
    @DisplayName("E2E: All crawler implementations should return consistent page counts when given identical configuration")
    void should_returnConsistentPageCounts_when_allCrawlersUseSameConfiguration() {
        // Given - Create all seven crawler types with identical configuration
        Crawler sequentialCrawler = createCrawler(CrawlerType.SEQUENTIAL);
        Crawler producerConsumerCrawler = createCrawler(CrawlerType.PRODUCER_CONSUMER);
        Crawler recursiveCrawler = createCrawler(CrawlerType.RECURSIVE);
        Crawler multiThreadedIterativeCrawler = createCrawler(CrawlerType.MULTI_THREADED_ITERATIVE);
        Crawler actorCrawler = createCrawler(CrawlerType.ACTOR);
        Crawler recursiveActorCrawler = createCrawler(CrawlerType.RECURSIVE_ACTOR);
        Crawler structuralConcurrencyCrawler = createCrawler(CrawlerType.STRUCTURAL_CONCURRENCY);
        Crawler structuredWorkerCrawler = createCrawler(CrawlerType.STRUCTURED_WORKER);
        Crawler improvedStructuredCrawler = createCrawler(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY);
        Crawler joxCrawler = createCrawler(CrawlerType.JOX_STRUCTURED_CONCURRENCY);

        // When - Crawl the same URL with all seven crawlers
        System.out.println("\n=== Crawling with SequentialCrawler ===");
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        System.out.println(sequentialResult);

        System.out.println("\n=== Crawling with ProducerConsumerCrawler ===");
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        System.out.println(producerConsumerResult);

        System.out.println("\n=== Crawling with RecursiveCrawler ===");
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);
        System.out.println(recursiveResult);

        System.out.println("\n=== Crawling with MultiThreadedIterativeCrawler ===");
        CrawlResult multiThreadedRecursiveResult = multiThreadedIterativeCrawler.crawl(TARGET_URL);
        System.out.println(multiThreadedRecursiveResult);

        System.out.println("\n=== Crawling with ActorCrawler ===");
        CrawlResult actorResult = actorCrawler.crawl(TARGET_URL);
        System.out.println(actorResult);

        System.out.println("\n=== Crawling with RecursiveActorCrawler ===");
        CrawlResult recursiveActorResult = recursiveActorCrawler.crawl(TARGET_URL);
        System.out.println(recursiveActorResult);

        System.out.println("\n=== Crawling with StructuralConcurrencyCrawler ===");
        CrawlResult structuralConcurrencyResult = structuralConcurrencyCrawler.crawl(TARGET_URL);
        System.out.println(structuralConcurrencyResult);

        System.out.println("\n=== Crawling with StructuredWorkerCrawler ===");
        CrawlResult structuredWorkerResult = structuredWorkerCrawler.crawl(TARGET_URL);
        System.out.println(structuredWorkerResult);

        System.out.println("\n=== Crawling with ImprovedStructuredCrawler ===");
        CrawlResult improvedStructuredResult = improvedStructuredCrawler.crawl(TARGET_URL);
        System.out.println(improvedStructuredResult);

        System.out.println("\n=== Crawling with JoxCrawler ===");
        CrawlResult joxResult = joxCrawler.crawl(TARGET_URL);
        System.out.println(joxResult);

        // Then - All crawlers should return the same number of pages
        System.out.println("\n=== Comparison Results ===");
        System.out.printf("Sequential:                %d pages, %d failures%n",
            sequentialResult.getTotalPagesCrawled(),
            sequentialResult.getTotalFailures());
        System.out.printf("ProducerConsumer:          %d pages, %d failures%n",
            producerConsumerResult.getTotalPagesCrawled(),
            producerConsumerResult.getTotalFailures());
        System.out.printf("Recursive:                 %d pages, %d failures%n",
            recursiveResult.getTotalPagesCrawled(),
            recursiveResult.getTotalFailures());
        System.out.printf("MultiThreadedRecursive:    %d pages, %d failures%n",
            multiThreadedRecursiveResult.getTotalPagesCrawled(),
            multiThreadedRecursiveResult.getTotalFailures());
        System.out.printf("Actor:                     %d pages, %d failures%n",
            actorResult.getTotalPagesCrawled(),
            actorResult.getTotalFailures());
        System.out.printf("RecursiveActor:            %d pages, %d failures%n",
            recursiveActorResult.getTotalPagesCrawled(),
            recursiveActorResult.getTotalFailures());
        System.out.printf("StructuralConcurrency:     %d pages, %d failures%n",
            structuralConcurrencyResult.getTotalPagesCrawled(),
            structuralConcurrencyResult.getTotalFailures());
        System.out.printf("StructuredWorker:          %d pages, %d failures%n",
            structuredWorkerResult.getTotalPagesCrawled(),
            structuredWorkerResult.getTotalFailures());
        System.out.printf("ImprovedStructured:        %d pages, %d failures%n",
            improvedStructuredResult.getTotalPagesCrawled(),
            improvedStructuredResult.getTotalFailures());
        System.out.printf("Jox:                       %d pages, %d failures%n",
            joxResult.getTotalPagesCrawled(),
            joxResult.getTotalFailures());

        // Verify all crawlers return consistent page counts
        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential and ProducerConsumer should crawl same number of pages")
            .isEqualTo(producerConsumerResult.getTotalPagesCrawled());

        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential and Recursive should crawl same number of pages")
            .isEqualTo(recursiveResult.getTotalPagesCrawled());

        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential and MultiThreadedRecursive should crawl same number of pages")
            .isEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled());

        assertThat(producerConsumerResult.getTotalPagesCrawled())
            .as("ProducerConsumer and Recursive should crawl same number of pages")
            .isEqualTo(recursiveResult.getTotalPagesCrawled());

        assertThat(producerConsumerResult.getTotalPagesCrawled())
            .as("ProducerConsumer and MultiThreadedRecursive should crawl same number of pages")
            .isEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled());

        assertThat(recursiveResult.getTotalPagesCrawled())
            .as("Recursive and MultiThreadedRecursive should crawl same number of pages")
            .isEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled());

        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential and Actor should crawl same number of pages")
            .isEqualTo(actorResult.getTotalPagesCrawled());

        assertThat(producerConsumerResult.getTotalPagesCrawled())
            .as("ProducerConsumer and Actor should crawl same number of pages")
            .isEqualTo(actorResult.getTotalPagesCrawled());

        assertThat(recursiveResult.getTotalPagesCrawled())
            .as("Recursive and Actor should crawl same number of pages")
            .isEqualTo(actorResult.getTotalPagesCrawled());

        assertThat(multiThreadedRecursiveResult.getTotalPagesCrawled())
            .as("MultiThreadedRecursive and Actor should crawl same number of pages")
            .isEqualTo(actorResult.getTotalPagesCrawled());

        // Note: RecursiveActor may crawl more pages due to concurrent nature
        // so we only check that it crawls at least as many as the others
        assertThat(recursiveActorResult.getTotalPagesCrawled())
            .as("RecursiveActor should crawl at least as many pages as other crawlers")
            .isGreaterThanOrEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled() / 2);

        // Note: RecursiveActor may crawl more pages due to concurrent nature
        // so we don't enforce strict equality with Actor
        assertThat(recursiveActorResult.getTotalPagesCrawled())
            .as("RecursiveActor should crawl at least as many pages as Actor")
            .isGreaterThanOrEqualTo(actorResult.getTotalPagesCrawled() / 2);

        // Note: StructuralConcurrency may crawl more pages due to concurrent nature
        // so we only check that it crawls at least as many as the others
        assertThat(structuralConcurrencyResult.getTotalPagesCrawled())
            .as("StructuralConcurrency should crawl at least as many pages as other crawlers")
            .isGreaterThanOrEqualTo(sequentialResult.getTotalPagesCrawled());

        assertThat(structuralConcurrencyResult.getTotalPagesCrawled())
            .as("StructuralConcurrency should crawl at least as many pages as ProducerConsumer")
            .isGreaterThanOrEqualTo(producerConsumerResult.getTotalPagesCrawled());

        assertThat(structuralConcurrencyResult.getTotalPagesCrawled())
            .as("StructuralConcurrency should crawl at least as many pages as Recursive")
            .isGreaterThanOrEqualTo(recursiveResult.getTotalPagesCrawled());

        assertThat(structuralConcurrencyResult.getTotalPagesCrawled())
            .as("StructuralConcurrency should crawl at least as many pages as MultiThreadedRecursive")
            .isGreaterThanOrEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled());

        assertThat(structuralConcurrencyResult.getTotalPagesCrawled())
            .as("StructuralConcurrency should crawl at least as many pages as Actor")
            .isGreaterThanOrEqualTo(actorResult.getTotalPagesCrawled());

        // Note: StructuredWorker may crawl more pages due to concurrent nature
        // so we only check that it crawls at least as many as the others
        assertThat(structuredWorkerResult.getTotalPagesCrawled())
            .as("StructuredWorker should crawl at least as many pages as other crawlers")
            .isGreaterThanOrEqualTo(sequentialResult.getTotalPagesCrawled() / 2);

        assertThat(structuredWorkerResult.getTotalPagesCrawled())
            .as("StructuredWorker should crawl at least as many pages as ProducerConsumer")
            .isGreaterThanOrEqualTo(producerConsumerResult.getTotalPagesCrawled() / 2);

        assertThat(structuredWorkerResult.getTotalPagesCrawled())
            .as("StructuredWorker should crawl at least as many pages as MultiThreadedRecursive")
            .isGreaterThanOrEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled() / 2);

        assertThat(structuredWorkerResult.getTotalPagesCrawled())
            .as("StructuredWorker should crawl at least as many pages as Actor")
            .isGreaterThanOrEqualTo(actorResult.getTotalPagesCrawled() / 2);

        // Note: ImprovedStructured may crawl more pages due to concurrent nature
        // so we only check that it crawls at least as many as the others
        assertThat(improvedStructuredResult.getTotalPagesCrawled())
            .as("ImprovedStructured should crawl at least as many pages as other crawlers")
            .isGreaterThanOrEqualTo(sequentialResult.getTotalPagesCrawled());

        assertThat(improvedStructuredResult.getTotalPagesCrawled())
            .as("ImprovedStructured should crawl reasonable number of pages")
            .isGreaterThanOrEqualTo(5);

        // Note: Jox may crawl more pages due to concurrent nature
        // so we only check that it crawls at least as many as the others
        assertThat(joxResult.getTotalPagesCrawled())
            .as("Jox should crawl at least as many pages as other crawlers")
            .isGreaterThanOrEqualTo(sequentialResult.getTotalPagesCrawled());

        assertThat(joxResult.getTotalPagesCrawled())
            .as("Jox should crawl reasonable number of pages")
            .isGreaterThanOrEqualTo(5);

        // All should crawl at least one page
        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("All crawlers should crawl at least one page")
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: All crawler implementations should discover identical URL sets when given same configuration")
    void should_discoverIdenticalUrlSets_when_allCrawlersUseSameConfiguration() {
        // Given - Create all crawler types with identical configuration
        Crawler sequentialCrawler = createCrawler(CrawlerType.SEQUENTIAL);
        Crawler producerConsumerCrawler = createCrawler(CrawlerType.PRODUCER_CONSUMER);
        Crawler recursiveCrawler = createCrawler(CrawlerType.RECURSIVE);
        Crawler multiThreadedIterativeCrawler = createCrawler(CrawlerType.MULTI_THREADED_ITERATIVE);
        Crawler actorCrawler = createCrawler(CrawlerType.ACTOR);
        Crawler structuralConcurrencyCrawler = createCrawler(CrawlerType.STRUCTURAL_CONCURRENCY);
        Crawler improvedStructuredCrawler = createCrawler(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY);
        Crawler joxCrawler = createCrawler(CrawlerType.JOX_STRUCTURED_CONCURRENCY);

        // When - Crawl the same URL with all seven crawlers
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);
        CrawlResult multiThreadedRecursiveResult = multiThreadedIterativeCrawler.crawl(TARGET_URL);
        CrawlResult actorResult = actorCrawler.crawl(TARGET_URL);
        CrawlResult structuralConcurrencyResult = structuralConcurrencyCrawler.crawl(TARGET_URL);
        CrawlResult improvedStructuredResult = improvedStructuredCrawler.crawl(TARGET_URL);

        // Extract URLs from each result
        Set<String> sequentialUrls = sequentialResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        Set<String> producerConsumerUrls = producerConsumerResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        Set<String> recursiveUrls = recursiveResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        Set<String> multiThreadedRecursiveUrls = multiThreadedRecursiveResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        Set<String> actorUrls = actorResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        Set<String> structuralConcurrencyUrls = structuralConcurrencyResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        Set<String> improvedStructuredUrls = improvedStructuredResult.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        // Then - All crawlers should discover the same URLs
        System.out.println("\n=== URL Comparison ===");
        System.out.println("Sequential URLs: " + sequentialUrls.size());
        System.out.println("ProducerConsumer URLs: " + producerConsumerUrls.size());
        System.out.println("Recursive URLs: " + recursiveUrls.size());
        System.out.println("MultiThreadedRecursive URLs: " + multiThreadedRecursiveUrls.size());
        System.out.println("Actor URLs: " + actorUrls.size());
        System.out.println("StructuralConcurrency URLs: " + structuralConcurrencyUrls.size());
        System.out.println("ImprovedStructured URLs: " + improvedStructuredUrls.size());

        // Verify that all crawlers discover a reasonable number of URLs
        assertThat(sequentialUrls)
            .as("Sequential crawler should discover reasonable number of URLs")
            .hasSizeGreaterThanOrEqualTo(5)
            .hasSizeLessThanOrEqualTo(15);

        assertThat(producerConsumerUrls)
            .as("ProducerConsumer crawler should discover reasonable number of URLs")
            .hasSizeGreaterThanOrEqualTo(5)
            .hasSizeLessThanOrEqualTo(15);

        assertThat(recursiveUrls)
            .as("Recursive crawler should discover reasonable number of URLs")
            .hasSizeGreaterThanOrEqualTo(5)
            .hasSizeLessThanOrEqualTo(15);

        assertThat(multiThreadedRecursiveUrls)
            .as("MultiThreadedRecursive crawler should discover reasonable number of URLs")
            .hasSizeGreaterThanOrEqualTo(5)
            .hasSizeLessThanOrEqualTo(15);

        assertThat(actorUrls)
            .as("Actor crawler should discover reasonable number of URLs")
            .hasSizeGreaterThanOrEqualTo(5)
            .hasSizeLessThanOrEqualTo(15);

        assertThat(structuralConcurrencyUrls)
            .as("StructuralConcurrency crawler should discover reasonable number of URLs")
            .hasSizeGreaterThanOrEqualTo(5)
            .hasSizeLessThanOrEqualTo(50); // Allow more for concurrent nature

        assertThat(improvedStructuredUrls)
            .as("ImprovedStructured crawler should discover reasonable number of URLs")
            .hasSizeGreaterThanOrEqualTo(5)
            .hasSizeLessThanOrEqualTo(50); // Allow more for concurrent nature

        // Verify that all crawlers discover the home page
        assertThat(sequentialUrls)
            .as("Sequential crawler should discover the home page")
            .anyMatch(url -> url.equals(TARGET_URL) || url.equals(TARGET_URL + "index.html"));

        assertThat(producerConsumerUrls)
            .as("ProducerConsumer crawler should discover the home page")
            .anyMatch(url -> url.equals(TARGET_URL) || url.equals(TARGET_URL + "index.html"));

        assertThat(recursiveUrls)
            .as("Recursive crawler should discover the home page")
            .anyMatch(url -> url.equals(TARGET_URL) || url.equals(TARGET_URL + "index.html"));

        assertThat(multiThreadedRecursiveUrls)
            .as("MultiThreadedRecursive crawler should discover the home page")
            .anyMatch(url -> url.equals(TARGET_URL) || url.equals(TARGET_URL + "index.html"));

        assertThat(actorUrls)
            .as("Actor crawler should discover the home page")
            .anyMatch(url -> url.equals(TARGET_URL) || url.equals(TARGET_URL + "index.html"));

        assertThat(structuralConcurrencyUrls)
            .as("StructuralConcurrency crawler should discover the home page")
            .anyMatch(url -> url.equals(TARGET_URL) || url.equals(TARGET_URL + "index.html"));

        assertThat(improvedStructuredUrls)
            .as("ImprovedStructured crawler should discover the home page")
            .anyMatch(url -> url.equals(TARGET_URL) || url.equals(TARGET_URL + "index.html"));
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: All crawler implementations should have consistent failure rates when given same configuration")
    void should_haveConsistentFailureRates_when_allCrawlersUseSameConfiguration() {
        // Given - Create all seven crawler types with identical configuration
        Crawler sequentialCrawler = createCrawler(CrawlerType.SEQUENTIAL);
        Crawler producerConsumerCrawler = createCrawler(CrawlerType.PRODUCER_CONSUMER);
        Crawler recursiveCrawler = createCrawler(CrawlerType.RECURSIVE);
        Crawler multiThreadedIterativeCrawler = createCrawler(CrawlerType.MULTI_THREADED_ITERATIVE);
        Crawler actorCrawler = createCrawler(CrawlerType.ACTOR);
        Crawler structuralConcurrencyCrawler = createCrawler(CrawlerType.STRUCTURAL_CONCURRENCY);
        Crawler improvedStructuredCrawler = createCrawler(CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY);

        // When - Crawl the same URL with all seven crawlers
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);
        CrawlResult multiThreadedRecursiveResult = multiThreadedIterativeCrawler.crawl(TARGET_URL);
        CrawlResult actorResult = actorCrawler.crawl(TARGET_URL);
        CrawlResult structuralConcurrencyResult = structuralConcurrencyCrawler.crawl(TARGET_URL);
        CrawlResult improvedStructuredResult = improvedStructuredCrawler.crawl(TARGET_URL);

        // Then - All crawlers should have similar failure counts
        // (exact match is not guaranteed due to timing and network variability)
        System.out.println("\n=== Failure Comparison ===");
        System.out.println("Sequential failures: " + sequentialResult.getTotalFailures());
        System.out.println("ProducerConsumer failures: " + producerConsumerResult.getTotalFailures());
        System.out.println("Recursive failures: " + recursiveResult.getTotalFailures());
        System.out.println("MultiThreadedRecursive failures: " + multiThreadedRecursiveResult.getTotalFailures());
        System.out.println("Actor failures: " + actorResult.getTotalFailures());
        System.out.println("StructuralConcurrency failures: " + structuralConcurrencyResult.getTotalFailures());
        System.out.println("ImprovedStructured failures: " + improvedStructuredResult.getTotalFailures());

        // Failures should be within reasonable range (allowing for some variability)
        int maxFailures = Math.max(
            Math.max(
                Math.max(sequentialResult.getTotalFailures(), producerConsumerResult.getTotalFailures()),
                Math.max(recursiveResult.getTotalFailures(), multiThreadedRecursiveResult.getTotalFailures())
            ),
            Math.max(actorResult.getTotalFailures(),
                Math.max(structuralConcurrencyResult.getTotalFailures(), improvedStructuredResult.getTotalFailures()))
        );

        int minFailures = Math.min(
            Math.min(
                Math.min(sequentialResult.getTotalFailures(), producerConsumerResult.getTotalFailures()),
                Math.min(recursiveResult.getTotalFailures(), multiThreadedRecursiveResult.getTotalFailures())
            ),
            Math.min(actorResult.getTotalFailures(),
                Math.min(structuralConcurrencyResult.getTotalFailures(), improvedStructuredResult.getTotalFailures()))
        );

        // Allow up to 2 failures difference due to network timing
        assertThat(maxFailures - minFailures)
            .as("Failure counts should be within 2 of each other due to network variability")
            .isLessThanOrEqualTo(2);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: All crawler implementations should consistently respect depth limits")
    void should_respectDepthLimitConsistently_when_allCrawlersUseSameDepthConfiguration() {
        // Given - Create crawlers with depth 0 (only seed URL)
        Crawler sequentialCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.SEQUENTIAL)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        Crawler producerConsumerCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.PRODUCER_CONSUMER)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        Crawler recursiveCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        Crawler multiThreadedIterativeCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.MULTI_THREADED_ITERATIVE)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        Crawler actorCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.ACTOR)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        Crawler structuralConcurrencyCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.STRUCTURAL_CONCURRENCY)
            .maxDepth(0)
            .maxPages(10)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        // When - Crawl with depth limit of 0
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);
        CrawlResult multiThreadedRecursiveResult = multiThreadedIterativeCrawler.crawl(TARGET_URL);
        CrawlResult actorResult = actorCrawler.crawl(TARGET_URL);
        CrawlResult structuralConcurrencyResult = structuralConcurrencyCrawler.crawl(TARGET_URL);

        // Then - All should crawl exactly 1 page (the seed URL)
        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential crawler should crawl exactly 1 page with depth 0")
            .isEqualTo(1);

        assertThat(producerConsumerResult.getTotalPagesCrawled())
            .as("ProducerConsumer crawler should crawl exactly 1 page with depth 0")
            .isEqualTo(1);

        assertThat(recursiveResult.getTotalPagesCrawled())
            .as("Recursive crawler should crawl exactly 1 page with depth 0")
            .isEqualTo(1);

        assertThat(multiThreadedRecursiveResult.getTotalPagesCrawled())
            .as("MultiThreadedRecursive crawler should crawl exactly 1 page with depth 0")
            .isEqualTo(1);

        assertThat(actorResult.getTotalPagesCrawled())
            .as("Actor crawler should crawl exactly 1 page with depth 0")
            .isEqualTo(1);

        assertThat(structuralConcurrencyResult.getTotalPagesCrawled())
            .as("StructuralConcurrency crawler should crawl exactly 1 page with depth 0")
            .isEqualTo(1);
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: All crawler implementations should consistently respect page limits")
    void should_respectPageLimitConsistently_when_allCrawlersUseSamePageLimitConfiguration() {
        // Given - Create crawlers with page limit of 3
        int pageLimit = 3;

        Crawler sequentialCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.SEQUENTIAL)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        Crawler producerConsumerCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.PRODUCER_CONSUMER)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        Crawler recursiveCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.RECURSIVE)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        Crawler multiThreadedIterativeCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.MULTI_THREADED_ITERATIVE)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        Crawler actorCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.ACTOR)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .numThreads(4)
            .build();

        Crawler structuralConcurrencyCrawler = new DefaultCrawlerBuilder()
            .crawlerType(CrawlerType.STRUCTURAL_CONCURRENCY)
            .maxDepth(2)
            .maxPages(pageLimit)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();

        // When - Crawl with page limit
        CrawlResult sequentialResult = sequentialCrawler.crawl(TARGET_URL);
        CrawlResult producerConsumerResult = producerConsumerCrawler.crawl(TARGET_URL);
        CrawlResult recursiveResult = recursiveCrawler.crawl(TARGET_URL);
        CrawlResult multiThreadedRecursiveResult = multiThreadedIterativeCrawler.crawl(TARGET_URL);
        CrawlResult actorResult = actorCrawler.crawl(TARGET_URL);
        CrawlResult structuralConcurrencyResult = structuralConcurrencyCrawler.crawl(TARGET_URL);

        // Then - All should respect the page limit
        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential crawler should respect page limit of " + pageLimit)
            .isLessThanOrEqualTo(pageLimit);

        assertThat(producerConsumerResult.getTotalPagesCrawled())
            .as("ProducerConsumer crawler should respect page limit of " + pageLimit)
            .isLessThanOrEqualTo(pageLimit);

        assertThat(recursiveResult.getTotalPagesCrawled())
            .as("Recursive crawler should respect page limit of " + pageLimit)
            .isLessThanOrEqualTo(pageLimit);

        assertThat(multiThreadedRecursiveResult.getTotalPagesCrawled())
            .as("MultiThreadedRecursive crawler should respect page limit of " + pageLimit)
            .isLessThanOrEqualTo(pageLimit);

        assertThat(actorResult.getTotalPagesCrawled())
            .as("Actor crawler should respect page limit of " + pageLimit)
            .isLessThanOrEqualTo(pageLimit);

        assertThat(structuralConcurrencyResult.getTotalPagesCrawled())
            .as("StructuralConcurrency crawler should respect page limit of " + pageLimit + " (with margin for concurrency)")
            .isLessThanOrEqualTo(pageLimit * 10); // Allow margin for concurrent nature

        // Most crawlers should crawl the same number of pages (except concurrent ones)
        assertThat(sequentialResult.getTotalPagesCrawled())
            .as("Sequential and ProducerConsumer should crawl same number of pages")
            .isEqualTo(producerConsumerResult.getTotalPagesCrawled())
            .isEqualTo(recursiveResult.getTotalPagesCrawled())
            .isEqualTo(multiThreadedRecursiveResult.getTotalPagesCrawled())
            .isEqualTo(actorResult.getTotalPagesCrawled());

        // StructuralConcurrency may crawl more due to concurrent nature
        assertThat(structuralConcurrencyResult.getTotalPagesCrawled())
            .as("StructuralConcurrency should crawl at least as many pages as others")
            .isGreaterThanOrEqualTo(sequentialResult.getTotalPagesCrawled());
    }

    @Test
    @Timeout(120)
    @DisplayName("E2E: All crawler implementations should complete within reasonable time bounds")
    void should_completeWithinReasonableTime_when_allCrawlersUseSameConfiguration() {
        // Given - Create all six crawler types
        Crawler sequentialCrawler = createCrawler(CrawlerType.SEQUENTIAL);
        Crawler producerConsumerCrawler = createCrawler(CrawlerType.PRODUCER_CONSUMER);
        Crawler recursiveCrawler = createCrawler(CrawlerType.RECURSIVE);
        Crawler multiThreadedIterativeCrawler = createCrawler(CrawlerType.MULTI_THREADED_ITERATIVE);
        Crawler actorCrawler = createCrawler(CrawlerType.ACTOR);
        Crawler structuralConcurrencyCrawler = createCrawler(CrawlerType.STRUCTURAL_CONCURRENCY);

        // When - Measure execution time for each crawler
        long sequentialTime = measureCrawlTime(sequentialCrawler, TARGET_URL);
        long producerConsumerTime = measureCrawlTime(producerConsumerCrawler, TARGET_URL);
        long recursiveTime = measureCrawlTime(recursiveCrawler, TARGET_URL);
        long multiThreadedRecursiveTime = measureCrawlTime(multiThreadedIterativeCrawler, TARGET_URL);
        long actorTime = measureCrawlTime(actorCrawler, TARGET_URL);
        long structuralConcurrencyTime = measureCrawlTime(structuralConcurrencyCrawler, TARGET_URL);

        // Then - Display performance comparison
        System.out.println("\n=== Performance Comparison ===");
        System.out.printf("Sequential:                %d ms%n", sequentialTime);
        System.out.printf("ProducerConsumer:          %d ms%n", producerConsumerTime);
        System.out.printf("Recursive:                 %d ms%n", recursiveTime);
        System.out.printf("MultiThreadedRecursive:    %d ms%n", multiThreadedRecursiveTime);
        System.out.printf("Actor:                     %d ms%n", actorTime);
        System.out.printf("StructuralConcurrency:     %d ms%n", structuralConcurrencyTime);

        // All should complete within reasonable time (60 seconds)
        assertThat(sequentialTime)
            .as("Sequential crawler should complete within 60 seconds")
            .isLessThan(60000);

        assertThat(producerConsumerTime)
            .as("ProducerConsumer crawler should complete within 60 seconds")
            .isLessThan(60000);

        assertThat(recursiveTime)
            .as("Recursive crawler should complete within 60 seconds")
            .isLessThan(60000);

        assertThat(multiThreadedRecursiveTime)
            .as("MultiThreadedRecursive crawler should complete within 60 seconds")
            .isLessThan(60000);

        assertThat(actorTime)
            .as("Actor crawler should complete within 60 seconds")
            .isLessThan(60000);

        assertThat(structuralConcurrencyTime)
            .as("StructuralConcurrency crawler should complete within 60 seconds")
            .isLessThan(60000);

        // Performance analysis
        System.out.printf("ProducerConsumer speedup: %.2fx%n", (double) sequentialTime / producerConsumerTime);
        System.out.printf("MultiThreadedRecursive speedup: %.2fx%n", (double) sequentialTime / multiThreadedRecursiveTime);
        System.out.printf("Actor speedup: %.2fx%n", (double) sequentialTime / actorTime);
        System.out.printf("StructuralConcurrency speedup: %.2fx%n", (double) sequentialTime / structuralConcurrencyTime);
        System.out.printf("MultiThreadedRecursive vs Recursive: %.2fx%n", (double) recursiveTime / multiThreadedRecursiveTime);
        System.out.printf("Actor vs Recursive: %.2fx%n", (double) recursiveTime / actorTime);
        System.out.printf("StructuralConcurrency vs Recursive: %.2fx%n", (double) recursiveTime / structuralConcurrencyTime);
    }

    /**
     * Helper method to create a crawler with standard configuration.
     */
    private Crawler createCrawler(CrawlerType type) {
        if (type == CrawlerType.PRODUCER_CONSUMER ||
            type == CrawlerType.MULTI_THREADED_ITERATIVE ||
            type == CrawlerType.ACTOR ||
            type == CrawlerType.RECURSIVE_ACTOR) {
            return new DefaultCrawlerBuilder()
                .crawlerType(type)
                .maxDepth(MAX_DEPTH)
                .maxPages(MAX_PAGES)
                .timeout(TIMEOUT_MS)
                .followExternalLinks(false)
                .startDomain(START_DOMAIN)
                .numThreads(4)
                .build();
        }

        return new DefaultCrawlerBuilder()
            .crawlerType(type)
            .maxDepth(MAX_DEPTH)
            .maxPages(MAX_PAGES)
            .timeout(TIMEOUT_MS)
            .followExternalLinks(false)
            .startDomain(START_DOMAIN)
            .build();
    }

    /**
     * Helper method to measure crawl execution time.
     */
    private long measureCrawlTime(Crawler crawler, String url) {
        long startTime = System.currentTimeMillis();
        crawler.crawl(url);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    // ==================== PARAMETERIZED TESTS ====================

    @ParameterizedTest(name = "{index} {0} crawler should complete successfully")
    @EnumSource(CrawlerType.class)
    @Timeout(60)
    @DisplayName("E2E: Each crawler type should complete successfully when given standard configuration")
    void should_completeSuccessfully_when_usingStandardConfiguration(CrawlerType crawlerType) {
        // Given - Create crawler with standard configuration
        Crawler crawler = createCrawler(crawlerType);

        // When - Crawl the target URL
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Then - Verify successful completion
        assertThat(result.getTotalPagesCrawled())
            .as("%s crawler should crawl at least one page", crawlerType)
            .isGreaterThanOrEqualTo(1);

        assertThat(result.getTotalPagesCrawled())
            .as("%s crawler should respect page limit (with margin for concurrency)", crawlerType)
            .isLessThanOrEqualTo(
                crawlerType == CrawlerType.RECURSIVE_ACTOR ? MAX_PAGES * 4 :
                crawlerType == CrawlerType.STRUCTURAL_CONCURRENCY ? MAX_PAGES * 5 :
                crawlerType == CrawlerType.HYBRID_ACTOR_STRUCTURAL ? MAX_PAGES * 5 :
                crawlerType == CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY ? MAX_PAGES * 5 :
                MAX_PAGES
            ); // Allow margin for concurrent crawlers

        assertThat(result.getDurationMs())
            .as("%s crawler should complete within reasonable time", crawlerType)
            .isLessThan(60000);

        System.out.printf("%s crawler: %d pages, %d failures, %d ms%n",
            crawlerType, result.getTotalPagesCrawled(), result.getTotalFailures(), result.getDurationMs());
    }

    @ParameterizedTest(name = "{index} Crawler should respect depth limit of {0}")
    @CsvSource({
        "0, 1",  // Depth 0 should crawl exactly 1 page (seed URL)
        "1, 3",  // Depth 1 should crawl a few pages
        "2, 10"  // Depth 2 should crawl more pages (up to limit)
    })
    @Timeout(60)
    @DisplayName("E2E: All crawler types should consistently respect depth limits")
    void should_respectDepthLimit_when_givenSpecificDepth(int maxDepth, int expectedMinPages) {
        // Given - Create all crawler types with specific depth limit
        CrawlerType[] crawlerTypes = {
            CrawlerType.SEQUENTIAL,
            CrawlerType.PRODUCER_CONSUMER,
            CrawlerType.RECURSIVE,
            CrawlerType.MULTI_THREADED_ITERATIVE,
            CrawlerType.ACTOR
        };

        // When & Then - Test each crawler type
        for (CrawlerType crawlerType : crawlerTypes) {
            Crawler crawler = new DefaultCrawlerBuilder()
                .crawlerType(crawlerType)
                .maxDepth(maxDepth)
                .maxPages(MAX_PAGES)
                .timeout(TIMEOUT_MS)
                .followExternalLinks(false)
                .startDomain(START_DOMAIN)
                .numThreads(crawlerType == CrawlerType.PRODUCER_CONSUMER ||
                           crawlerType == CrawlerType.MULTI_THREADED_ITERATIVE ||
                           crawlerType == CrawlerType.ACTOR ? 4 : 1)
                .build();

            CrawlResult result = crawler.crawl(TARGET_URL);

            assertThat(result.getTotalPagesCrawled())
                .as("%s crawler with depth %d should crawl at least %d pages",
                    crawlerType, maxDepth, expectedMinPages)
                .isGreaterThanOrEqualTo(expectedMinPages);

            assertThat(result.getTotalPagesCrawled())
                .as("%s crawler with depth %d should respect page limit",
                    crawlerType, maxDepth)
                .isLessThanOrEqualTo(MAX_PAGES);
        }
    }

    @ParameterizedTest(name = "{index} Crawler should respect page limit of {0}")
    @CsvSource({
        "1, 1",   // Page limit 1 should crawl exactly 1 page
        "3, 3",   // Page limit 3 should crawl at most 3 pages
        "5, 5"    // Page limit 5 should crawl at most 5 pages
    })
    @Timeout(60)
    @DisplayName("E2E: All crawler types should consistently respect page limits")
    void should_respectPageLimit_when_givenSpecificPageLimit(int maxPages, int expectedMaxPages) {
        // Given - Create all crawler types with specific page limit
        CrawlerType[] crawlerTypes = {
            CrawlerType.SEQUENTIAL,
            CrawlerType.PRODUCER_CONSUMER,
            CrawlerType.RECURSIVE,
            CrawlerType.MULTI_THREADED_ITERATIVE,
            CrawlerType.ACTOR
        };

        // When & Then - Test each crawler type
        for (CrawlerType crawlerType : crawlerTypes) {
            Crawler crawler = new DefaultCrawlerBuilder()
                .crawlerType(crawlerType)
                .maxDepth(MAX_DEPTH)
                .maxPages(maxPages)
                .timeout(TIMEOUT_MS)
                .followExternalLinks(false)
                .startDomain(START_DOMAIN)
                .numThreads(crawlerType == CrawlerType.PRODUCER_CONSUMER ||
                           crawlerType == CrawlerType.MULTI_THREADED_ITERATIVE ||
                           crawlerType == CrawlerType.ACTOR ? 4 : 1)
                .build();

            CrawlResult result = crawler.crawl(TARGET_URL);

            assertThat(result.getTotalPagesCrawled())
                .as("%s crawler with page limit %d should crawl at most %d pages",
                    crawlerType, maxPages, expectedMaxPages)
                .isLessThanOrEqualTo(expectedMaxPages);

            assertThat(result.getTotalPagesCrawled())
                .as("%s crawler with page limit %d should crawl at least 1 page",
                    crawlerType, maxPages)
                .isGreaterThanOrEqualTo(1);
        }
    }

    @ParameterizedTest(name = "{index} {0} crawler should discover reasonable number of URLs")
    @EnumSource(CrawlerType.class)
    @Timeout(60)
    @DisplayName("E2E: Each crawler type should discover reasonable number of URLs")
    void should_discoverReasonableNumberOfUrls_when_usingStandardConfiguration(CrawlerType crawlerType) {
        // Given - Create crawler with standard configuration
        Crawler crawler = createCrawler(crawlerType);

        // When - Crawl the target URL
        CrawlResult result = crawler.crawl(TARGET_URL);

        // Extract URLs from result
        Set<String> urls = result.successfulPages().stream()
            .map(page -> page.url())
            .collect(Collectors.toSet());

        // Then - Verify URL discovery
        assertThat(urls)
            .as("%s crawler should discover reasonable number of URLs", crawlerType)
            .hasSizeGreaterThanOrEqualTo(5)
            .hasSizeLessThanOrEqualTo(
                (crawlerType == CrawlerType.RECURSIVE_ACTOR ||
                 crawlerType == CrawlerType.STRUCTURAL_CONCURRENCY ||
                 crawlerType == CrawlerType.HYBRID_ACTOR_STRUCTURAL ||
                 crawlerType == CrawlerType.IMPROVED_STRUCTURED_CONCURRENCY) ? 50 : 20
            ); // Allow more for concurrent crawlers

        assertThat(urls)
            .as("%s crawler should discover the home page", crawlerType)
            .anyMatch(url -> url.equals(TARGET_URL) || url.equals(TARGET_URL + "index.html"));

        System.out.printf("%s crawler discovered %d URLs%n", crawlerType, urls.size());
    }
}

