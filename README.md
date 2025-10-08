# Java Crawler Demo

A web crawler implementations in Java.

## Design alternatives

- [V1: Sequential](./docs/sequential-crawler-overview.png)
- [V2: Producer-Consumer Pattern (Executor Service)](./docs/producer-consumer-crawler-overview.png)
- [V3: Recursive Design](./docs/recursive-crawler-overview.png)
- [V4: Recursive Design with Executor Service](./docs/multi-threaded-recursive-crawler-overview.png)
- [V5: Actor Model (Message-Passing with Executor Service)](./docs/actor-model-crawler-overview.png)
- [V6: Recursive Actor Model (Hybrid Approach)](./docs/recursive-actor-crawler-overview.png)
- [V7: Structural Concurrency (Java 25 Preview)](./docs/structural-concurrency-crawler-overview.png)
- Pipeline/Chain of Responsibility
- Strategy Pattern (Pluggable Components)
- Visitor Pattern (Content Processing)

## How to test

```bash
# Run E2E tests (requires Java 25 with preview features for V7)
./mvnw test -Pe2e -Dtest.e2e=true -Dtest=ComparisonE2ETest

./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v1.SequentialCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v2.ProducerConsumerCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v3.RecursiveCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v4.MultiThreadedRecursiveCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v5.ActorCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v6.RecursiveActorCrawlerExample"
# V7 requires Java 25 with preview features enabled
# Run directly with Java (Maven exec plugin doesn't work well with preview features)
./mvnw compile -Pexamples
java --enable-preview -cp target/classes:$(./mvnw dependency:build-classpath \
-q -Dmdep.outputFile=/dev/stdout)    info.jab.crawler.v7.StructuralConcurrencyCrawlerExample
```

## Java 25 Structural Concurrency (V7)

The V7 implementation demonstrates Java 25's structural concurrency features:

### Requirements
- Java 25 (Early Access) with preview features enabled
- `--enable-preview` flag for compilation and execution

### Key Benefits
- **Automatic Resource Management**: StructuredTaskScope automatically cleans up resources when scope closes
- **Simplified Error Handling**: Exceptions are properly propagated and handled within scopes
- **Natural Tree Structure**: Recursive crawling with proper scoping boundaries
- **Virtual Thread Efficiency**: Leverages virtual threads for optimal concurrency
- **Fault Isolation**: Failures in one branch don't affect other concurrent operations
- **Structured Scoping**: Clear boundaries for concurrent operations

**Note**: The Maven exec plugin doesn't work reliably with Java 25 preview features, so direct Java execution is recommended for V7.

### Maven Configuration
The `pom.xml` is configured to automatically enable preview features for:
- **Compiler Plugin**: Uses `maven.compiler.compilerArgs=--enable-preview`
- **Surefire Plugin**: Uses `argLine=--enable-preview` for unit tests
- **Failsafe Plugin**: Uses `argLine=--enable-preview` for integration/E2E tests
- **Exec Plugin**: Uses `options=--enable-preview` for example execution

## Key Design Considerations Across All Approaches:

- URL Frontier Management: Queue, Priority Queue, or Database-backed
- Politeness: Rate limiting, respecting robots.txt
- Deduplication: Visited URL tracking (Set, Bloom Filter, Database)
- Error Handling: Retry logic, circuit breakers
- State Management: In-memory, persistent, or distributed
- Parsing Strategy: CSS selectors vs XPath vs DOM traversal
- Data Extraction: Direct extraction vs Visitor pattern
- Storage: In-memory, files, databases

Powered by [Cursor](https://www.cursor.com/) with ❤️ from [Madrid](https://www.google.com/maps/place/Community+of+Madrid,+Madrid/@40.4983324,-6.3162283,8z/data=!3m1!4b1!4m6!3m5!1s0xd41817a40e033b9:0x10340f3be4bc880!8m2!3d40.4167088!4d-3.5812692!16zL20vMGo0eGc?entry=ttu&g_ep=EgoyMDI1MDgxOC4wIKXMDSoASAFQAw%3D%3D)
