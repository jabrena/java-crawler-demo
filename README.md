# Java Crawler Demo

A repository about a collection of web crawler implementation in Java.

## Comparison Matrix

| Aspect | v9 | v4 | v2 | v5 | v1 | v3 | v7 | v8 | v6 |
|--------|----|----|----|----|----|----|----|----|----|
| **Analysis** | [+ Info](./docs/v9/README.md) | [+ Info](./docs/v4/README.md) | [+ Info](./docs/v2/README.md) | [+ Info](./docs/v5/README.md) | [+ Info](./docs/v1/README.md) | [+ Info](./docs/v3/README.md) | [+ Info](./docs/v7/README.md) | [+ Info](./docs/v8/README.md) | [+ Info](./docs/v6/README.md) |
| **Implementation** | [StructuredWorker](./src/main/java/info/jab/crawler/v9/StructuredWorkerCrawler.java) | [MultiThreadedRecursive](./src/main/java/info/jab/crawler/v4/MultiThreadedRecursiveCrawler.java) | [ProducerConsumer](./src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [Actor](./src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [Sequential](./src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [Recursive](./src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [StructuralConcurrency](./src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) | [HybridActorStructural](./src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) | [RecursiveActor](./src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) |
| **Performance (JMH)** | **455.04 ms/op** ⭐ | 500.26 ms/op | 503.65 ms/op | 557.70 ms/op | 765.22 ms/op | 816.36 ms/op | 1570.43 ms/op | 1703.47 ms/op | 1873.83 ms/op ⚠️ |
| **Throughput** | **Highest** | **High** | High | High | Low | Low | Low | Low | Low |
| **Performance Rank** | **1st** | 2nd | 3rd | 4th | 5th | 6th | 7th | 8th | 9th |
| **Threading** | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (configurable) | Multi-threaded (configurable) | Single-threaded | Single-threaded | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (configurable) |
| **Complexity** | Medium | Very Complex | Complex | Very Complex | Simple | Medium | Medium | Complex | Very Complex |
| **Order** | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Scales with virtual threads | Scales with cores/threads | Scales with cores/threads | Scales with actors (distributed) | Limited | Limited | Scales with virtual threads | Scales with virtual threads + actors | Scales with actors (dynamic) |
| **Stack Safety** | Yes (structured scopes) | Yes (trampoline) | N/A | N/A | N/A | Yes (trampoline) | Yes (structured scopes) | Yes (structured scopes) | Yes (async recursion) |
| **Fault Tolerance** | High | Limited | Limited | High | None | None | High | Very High | High |
| **Performance Stability** | **Excellent** | **Excellent** | Good | Good | Good | Good | Fair | Fair | Poor |

## Conclusion

All nine crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements, with performance being a critical factor based on JMH benchmark results:

### 🏆 **Performance Leaders (Based on JMH Results)**
- **Choose Structured Worker (v9)** for **maximum performance** (455.04 ms/op) with modern Java 25 structured concurrency, virtual threads, and excellent stability
- **Choose Multi-threaded Recursive (v4)** for **near-optimal performance** (500.26 ms/op) with stack-safe deep recursion and excellent stability
- **Choose Producer-Consumer (v2)** for **good performance** (503.65 ms/op) with clear separation of concerns and proven scalability
- **Choose Actor (v5)** for **solid performance** (557.70 ms/op) with fault-tolerant, distributed systems and message-driven concurrency

### ⚖️ **Balanced Options**
- **Choose Sequential (v1)** for simplicity, predictability, and smaller crawling tasks (765.22 ms/op - 5th place)
- **Choose Recursive (v3)** for elegant functional programming and deep recursion without stack overflow (816.36 ms/op - 6th place)

### ⚠️ **Performance Concerns**
- **Avoid Recursive Actor (v6)** - Poor performance (1873.83 ms/op - 4.1x slower than best) with high variance
- **Avoid Hybrid implementations (v7, v8)** - Structural Concurrency (1570.43 ms/op) and Hybrid Actor-Structural (1703.47 ms/op) show significant overhead compared to the new leader

### 📊 **Key Performance Insights**
1. **Structured concurrency with virtual threads** (v9) achieves the best performance when combined with proven queue-based worker patterns
2. **Traditional Java concurrency** (ThreadPoolExecutor, BlockingQueue) remains highly competitive and stable
3. **Actor model overhead** can be significant for I/O-bound tasks like web crawling
4. **Hybrid approaches** introduce complexity without performance benefits
5. **Virtual threads** show excellent performance when properly integrated with structured concurrency primitives

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
