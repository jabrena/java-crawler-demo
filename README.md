# Java Crawler Demo

A web crawler implementations in Java.

## Comparison Matrix

| Aspect | v1 | v2 | v3 | v4 | v5 | v6 | v7 | v8 |
|--------|----|----|----|----|----|----|----|----|
| **Analysis** | [More](./docs/v1/README.md) | [More](./docs/v2/README.md) | [More](./docs/v3/README.md) | [More](./docs/v4/README.md) | [More](./docs/v5/README.md) | [More](./docs/v6/README.md) | [More](./docs/v7/README.md) | [More](./docs/v8/README.md) |
| **Implementation** | [SequentialCrawler](./src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [ProducerConsumerCrawler](./src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [RecursiveCrawler](./src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [MultiThreadedRecursiveCrawler](./src/main/java/info/jab/crawler/v4/MultiThreadedRecursiveCrawler.java) | [ActorCrawler](./src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [RecursiveActorCrawler](./src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) | [StructuralConcurrencyCrawler](./src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) | [HybridActorStructuralCrawler](./src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) |
| **Threading** | Single-threaded | Multi-threaded (configurable) | Single-threaded | Multi-threaded (configurable) | Multi-threaded (configurable) | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) |
| **Throughput** | Low | High | Low | **High** | High | Low | Low | Low |
| **Performance (JMH)** | 765.22 ms/op | 503.65 ms/op | 816.36 ms/op | **500.26 ms/op** ⭐ | 557.70 ms/op | 1873.83 ms/op ⚠️ | 1570.43 ms/op | 1703.47 ms/op |
| **Performance Rank** | 4th | 2nd | 5th | **1st** | 3rd | 8th | 6th | 7th |
| **Complexity** | Simple | Complex | Medium | Very Complex | Very Complex | Very Complex | Medium | Complex |
| **Order** | Deterministic | Non-deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Limited | Scales with cores/threads | Limited | Scales with cores/threads | Scales with actors (distributed) | Scales with actors (dynamic) | Scales with virtual threads | Scales with virtual threads + actors |
| **Stack Safety** | N/A | N/A | Yes (trampoline) | Yes (trampoline) | N/A | Yes (async recursion) | Yes (structured scopes) | Yes (structured scopes) |
| **Fault Tolerance** | None | Limited | None | Limited | High | High | High | Very High |
| **Performance Stability** | Good | Good | Good | **Excellent** | Good | Poor | Fair | Fair |

## Conclusion

All eight crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements, with performance being a critical factor based on JMH benchmark results:

### 🏆 **Performance Leaders (Based on JMH Results)**
- **Choose Multi-threaded Recursive (v4)** for **maximum performance** (500.26 ms/op) with stack-safe deep recursion and excellent stability
- **Choose Producer-Consumer (v2)** for **near-optimal performance** (503.65 ms/op) with clear separation of concerns and proven scalability
- **Choose Actor (v5)** for **good performance** (557.70 ms/op) with fault-tolerant, distributed systems and message-driven concurrency

### ⚖️ **Balanced Options**
- **Choose Sequential (v1)** for simplicity, predictability, and smaller crawling tasks (765.22 ms/op - 4th place)
- **Choose Recursive (v3)** for elegant functional programming and deep recursion without stack overflow (816.36 ms/op - 5th place)

### ⚠️ **Performance Concerns**
- **Avoid Recursive Actor (v6)** - Poor performance (1873.83 ms/op - 3.7x slower than best) with high variance
- **Avoid Hybrid implementations (v7, v8)** - Structural Concurrency (1570.43 ms/op) and Hybrid Actor-Structural (1703.47 ms/op) show significant overhead

### 📊 **Key Performance Insights**
1. **Traditional Java concurrency** (ThreadPoolExecutor, BlockingQueue) outperforms modern alternatives
2. **Actor model overhead** can be significant for I/O-bound tasks like web crawling
3. **Hybrid approaches** introduce complexity without performance benefits
4. **Structural concurrency** (Java 21+) needs optimization for this use case

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
# V8 Hybrid Actor-Structural Concurrency (requires Java 25 with preview features)
# Run directly with Java (Maven exec plugin doesn't work well with preview features)
./mvnw compile -Pexamples
java --enable-preview -cp target/classes:$(./mvnw dependency:build-classpath \
-q -Dmdep.outputFile=/dev/stdout)    info.jab.crawler.v8.HybridActorStructuralCrawlerExample
```

Powered by [Cursor](https://www.cursor.com/) with ❤️ from [Madrid](https://www.google.com/maps/place/Community+of+Madrid,+Madrid/@40.4983324,-6.3162283,8z/data=!3m1!4b1!4m6!3m5!1s0xd41817a40e033b9:0x10340f3be4bc880!8m2!3d40.4167088!4d-3.5812692!16zL20vMGo0eGc?entry=ttu&g_ep=EgoyMDI1MDgxOC4wIKXMDSoASAFQAw%3D%3D)
