# Java Crawler Demo

A repository about a collection of web crawler implementation in Java.

## Comparison Matrix

| Aspect | v5 | v2 | v3 | v1 | v4 | v9 | v6 | v8 | v7 |
|--------|----|----|----|----|----|----|----|----|----|
| **Analysis** | [+ Info](./docs/v5/README.md) | [+ Info](./docs/v2/README.md) | [+ Info](./docs/v3/README.md) | [+ Info](./docs/v1/README.md) | [+ Info](./docs/v4/README.md) | [+ Info](./docs/v9/README.md) | [+ Info](./docs/v6/README.md) | [+ Info](./docs/v8/README.md) | [+ Info](./docs/v7/README.md) |
| **Implementation** | [Actor](./src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [ProducerConsumer](./src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [Recursive](./src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [Sequential](./src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [MultiThreadedIterative](./src/main/java/info/jab/crawler/v4/MultiThreadedIterativeCrawler.java) | [StructuredWorker](./src/main/java/info/jab/crawler/v9/StructuredWorkerCrawler.java) | [RecursiveActor](./src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) | [HybridActorStructural](./src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) | [StructuralConcurrency](./src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) |
| **Performance (JMH)** | **342.23 ms/op** ⭐ | 360.92 ms/op | 409.16 ms/op | 414.38 ms/op | 437.68 ms/op | 455.04 ms/op | 610.11 ms/op ⚠️ | 1370.34 ms/op | 1714.99 ms/op |
| **Throughput** | **Highest** | **Highest** | High | High | High | High | Medium | Low | Low |
| **Performance Rank** | **1st** | 2nd | 3rd | 4th | 5th | 6th | 7th | 8th | 9th |
| **Threading** | Multi-threaded (configurable) | Multi-threaded (configurable) | Single-threaded | Single-threaded | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) |
| **Complexity** | Very Complex | Complex | Medium | Simple | Very Complex | Medium | Very Complex | Complex | Medium |
| **Order** | Non-deterministic | Non-deterministic | Deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Scales with actors (distributed) | Scales with cores/threads | Limited | Limited | Scales with cores/threads | Scales with virtual threads | Scales with actors (dynamic) | Scales with virtual threads + actors | Scales with virtual threads |
| **Stack Safety** | N/A | N/A | Yes (trampoline) | N/A | N/A | Yes (structured scopes) | Yes (async recursion) | Yes (structured scopes) | Yes (structured scopes) |
| **Fault Tolerance** | High | Limited | None | None | Limited | High | High | Very High | High |
| **Performance Stability** | Good | **Excellent** | Good | Good | **Excellent** | Good | Poor | Fair | Fair |

## Conclusion

All nine crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements, with performance being a critical factor based on JMH benchmark results:

### 🏆 **Performance Leaders (Based on JMH Results)**
- **Choose Actor (v5)** for **maximum performance** (342.23 ms/op) with fault-tolerant, distributed systems and message-driven concurrency
- **Choose Producer-Consumer (v2)** for **near-optimal performance** (360.92 ms/op) with clear separation of concerns and proven scalability
- **Choose Recursive (v3)** for **excellent performance** (409.16 ms/op) with elegant functional programming and deep recursion without stack overflow
- **Choose Sequential (v1)** for **solid performance** (414.38 ms/op) with simplicity, predictability, and smaller crawling tasks

### ⚖️ **Balanced Options**
- **Choose Multi-threaded Iterative (v4)** for good performance (437.68 ms/op - 5th place) with efficient iterative processing and excellent stability
- **Choose Structured Worker (v9)** for solid performance (455.04 ms/op - 6th place) with modern Java 25 structured concurrency and virtual threads

### ⚠️ **Performance Concerns**
- **Avoid Recursive Actor (v6)** - Poor performance (610.11 ms/op - 1.8x slower than best) with high variance
- **Avoid Hybrid implementations (v7, v8)** - Structural Concurrency (1714.99 ms/op) and Hybrid Actor-Structural (1370.34 ms/op) show significant overhead compared to the new leader

### 📊 **Key Performance Insights**
1. **Actor model** (v5) achieves the best performance (342.23 ms/op) with fault-tolerant, distributed systems and message-driven concurrency
2. **Traditional Java concurrency** (ThreadPoolExecutor, BlockingQueue) remains highly competitive and stable
3. **Recursive approaches** (v3) can be surprisingly efficient when properly implemented
4. **Hybrid approaches** (v7, v8) introduce complexity without performance benefits
5. **Virtual threads** (v9) show good performance but not the best when compared to traditional approaches

## How to test

```bash
# Run E2E tests (requires Java 25 with preview features for V7 and V8)
./mvnw test -Pe2e -Dtest.e2e=true -Dtest=ComparisonE2ETest

./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v1.SequentialCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v2.ProducerConsumerCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v3.RecursiveCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v4.MultiThreadedIterativeCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v5.ActorCrawlerExample"
./mvnw compile exec:java -Pexamples \
    -Dexec.mainClass="info.jab.crawler.v6.RecursiveActorCrawlerExample"
# V7 requires Java 25 with preview features enabled
# Run directly with Java (Maven exec plugin doesn't work well with preview features)
./mvnw compile -Pexamples
java --enable-preview -cp target/classes:$(./mvnw dependency:build-classpath \
-q -Dmdep.outputFile=/dev/stdout)    info.jab.crawler.v7.StructuralConcurrencyCrawlerExample
# V8 Hybrid Actor-Structural Concurrency (requires Java 25 with preview features)
# Run directly with Java (Maven exec plugin doesn't work well with preview features)
./mvnw compile -Pexamples
java --enable-preview -cp target/classes:$(./mvnw dependency:build-classpath \
-q -Dmdep.outputFile=/dev/stdout)    info.jab.crawler.v8.HybridActorStructuralCrawlerExample
# V9 Structured Worker (requires Java 25 with preview features)
# Run directly with Java (Maven exec plugin doesn't work well with preview features)
./mvnw compile -Pexamples
java --enable-preview -cp target/classes:$(./mvnw dependency:build-classpath \
-q -Dmdep.outputFile=/dev/stdout)    info.jab.crawler.v9.StructuredWorkerCrawlerExample
```

Powered by [Cursor](https://www.cursor.com/) with ❤️ from [Madrid](https://www.google.com/maps/place/Community+of+Madrid,+Madrid/@40.4983324,-6.3162283,8z/data=!3m1!4b1!4m6!3m5!1s0xd41817a40e033b9:0x10340f3be4bc880!8m2!3d40.4167088!4d-3.5812692!16zL20vMGo0eGc?entry=ttu&g_ep=EgoyMDI1MDgxOC4wIKXMDSoASAFQAw%3D%3D)
