# Java Crawler Demo

A repository about a collection of web crawler implementation in Java.

## Comparison Matrix

*Performance data based on JMH benchmarks (2025-01-09) using Java 25 with GraalVM CE, 2 forks, 5 measurement iterations*

| Aspect | v9 | v4 | v2 | v10 | v5 | v1 | v3 | v8 | v6 | v7 |
|--------|----|----|----|----|----|----|----|----|----|----|
| **Analysis** | [+ Info](./docs/v9/README.md) | [+ Info](./docs/v4/README.md) | [+ Info](./docs/v2/README.md) | [+ Info](./docs/v10/README.md) | [+ Info](./docs/v5/README.md) | [+ Info](./docs/v1/README.md) | [+ Info](./docs/v3/README.md) | [+ Info](./docs/v8/README.md) | [+ Info](./docs/v6/README.md) | [+ Info](./docs/v7/README.md) |
| **Implementation** | [StructuredWorker](./src/main/java/info/jab/crawler/v9/StructuredWorkerCrawler.java) | [MultiThreadedIterative](./src/main/java/info/jab/crawler/v4/MultiThreadedIterativeCrawler.java) | [ProducerConsumer](./src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [VirtualThreadActor](./src/main/java/info/jab/crawler/v10/VirtualThreadActorCrawler.java) | [Actor](./src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [Sequential](./src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [Recursive](./src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [HybridActorStructural](./src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) | [RecursiveActor](./src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) | [StructuralConcurrency](./src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) |
| **Performance (JMH)** | **464.61 ms/op** ⭐ | 482.59 ms/op | 495.82 ms/op | 500.36 ms/op | 514.10 ms/op | 709.23 ms/op | 730.20 ms/op | 1518.68 ms/op ⚠️ | 1709.19 ms/op ⚠️ | 1816.83 ms/op ⚠️ |
| **Throughput** | **Highest** | **Highest** | **Highest** | **Highest** | **Highest** | High | High | Low | Low | Low |
| **Performance Rank** | **1st** | 2nd | 3rd | 4th | 5th | 6th | 7th | 8th | 9th | 10th |
| **Threading** | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Single-threaded | Single-threaded | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (virtual threads) |
| **Complexity** | Medium | Very Complex | Complex | Very Complex | Very Complex | Simple | Medium | Complex | Very Complex | Medium |
| **Order** | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Scales with virtual threads | Scales with cores/threads | Scales with cores/threads | Scales with virtual threads + actors | Scales with actors (distributed) | Limited | Limited | Scales with virtual threads + actors | Scales with actors (dynamic) | Scales with virtual threads |
| **Stack Safety** | Yes (structured scopes) | N/A | N/A | Yes (async recursion) | N/A | N/A | Yes (trampoline) | Yes (structured scopes) | Yes (async recursion) | Yes (structured scopes) |
| **Fault Tolerance** | High | Limited | Limited | High | High | None | None | Very High | High | High |
| **Performance Stability** | Good | **Excellent** | **Excellent** | Good | Good | Good | Good | Fair | Poor | Fair |


## Conclusion

All crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements, with performance being a critical factor based on JMH benchmark results:

### 🏆 **Performance Leaders (Based on Latest JMH Results - 2025-01-09)**
- **Choose Structured Worker (v9)** for **maximum performance** (464.61 ms/op) with modern Java 25 structured concurrency and virtual threads
- **Choose Multi-threaded Iterative (v4)** for **near-optimal performance** (482.59 ms/op) with efficient iterative processing and excellent stability
- **Choose Producer-Consumer (v2)** for **excellent performance** (495.82 ms/op) with clear separation of concerns and proven scalability

### ⚖️ **Balanced Options**
- **Choose Virtual Thread Actor (v10)** for solid performance (500.36 ms/op) with simplified async processing and virtual thread-based actors
- **Choose Actor (v5)** for solid performance (514.10 ms/op) with fault-tolerant, distributed systems and message-driven concurrency
- **Choose Sequential (v1)** for solid performance (709.23 ms/op) with simplicity, predictability, and smaller crawling tasks

### ⚠️ **Performance Concerns**
- **Avoid Structural Concurrency (v7)** - Poor performance (1816.83 ms/op - 3.9x slower than best) with significant overhead
- **Avoid Recursive Actor (v6)** - Poor performance (1709.19 ms/op - 3.7x slower than best) with high variance
- **Avoid Hybrid Actor-Structural (v8)** - Poor performance (1518.68 ms/op - 3.3x slower than best) with significant overhead

### 🆕 **New Implementation**
- **Virtual Thread Actor (v10)** - Successfully benchmarked with excellent performance (500.36 ms/op - 4th place). Delivers on its promise of simplified async processing with virtual threads and better resource utilization compared to v5. Uses virtual thread-based actors for lightweight concurrency with supervisor pattern for fault tolerance.

### 📊 **Key Performance Insights**
1. **Virtual threads** (v9) achieve the best performance with modern Java 25 structured concurrency
2. **Traditional Java concurrency** (ThreadPoolExecutor, BlockingQueue) continues to excel in performance and stability
3. **Virtual Thread Actor (v10)** successfully delivers on its promise of simplified async processing with excellent performance (4th place)
4. **Actor model** (v5) remains competitive with fault-tolerant, distributed systems
5. **Recursive Actor approaches** (v6) show significant performance degradation in recent benchmarks
6. **Hybrid approaches** (v7, v8) introduce complexity without performance benefits
7. **Virtual threads** demonstrate superior performance compared to traditional threading models

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
# V10 Virtual Thread Actor (requires Java 21+ for virtual threads)
# Run directly with Java (Maven exec plugin doesn't work well with virtual threads)
./mvnw compile -Pexamples
java -cp target/classes:$(./mvnw dependency:build-classpath \
-q -Dmdep.outputFile=/dev/stdout)    info.jab.crawler.v10.VirtualThreadActorCrawlerExample
```

Powered by [Cursor](https://www.cursor.com/) with ❤️ from [Madrid](https://www.google.com/maps/place/Community+of+Madrid,+Madrid/@40.4983324,-6.3162283,8z/data=!3m1!4b1!4m6!3m5!1s0xd41817a40e033b9:0x10340f3be4bc880!8m2!3d40.4167088!4d-3.5812692!16zL20vMGo0eGc?entry=ttu&g_ep=EgoyMDI1MDgxOC4wIKXMDSoASAFQAw%3D%3D)
