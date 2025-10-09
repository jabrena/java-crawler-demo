# Java Crawler Demo

A repository about a collection of web crawler implementation in Java.

## Comparison Matrix

*Performance data based on JMH benchmarks (2025-01-09) using Java 25 with GraalVM CE, 2 forks, 5 measurement iterations*

| Aspect | v4 | v9 | v10 | v2 | v5 | v3 | v1 | v6 | v8 | v7 |
|--------|----|----|----|----|----|----|----|----|----|----|
| **Analysis** | [+ Info](./docs/v4/README.md) | [+ Info](./docs/v9/README.md) | [+ Info](./docs/v10/README.md) | [+ Info](./docs/v2/README.md) | [+ Info](./docs/v5/README.md) | [+ Info](./docs/v3/README.md) | [+ Info](./docs/v1/README.md) | [+ Info](./docs/v6/README.md) | [+ Info](./docs/v8/README.md) | [+ Info](./docs/v7/README.md) |
| **Implementation** | [MultiThreadedIterative](./src/main/java/info/jab/crawler/v4/MultiThreadedIterativeCrawler.java) | [StructuredWorker](./src/main/java/info/jab/crawler/v9/StructuredWorkerCrawler.java) | [VirtualThreadActor](./src/main/java/info/jab/crawler/v10/VirtualThreadActorCrawler.java) | [ProducerConsumer](./src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [Actor](./src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [Recursive](./src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [Sequential](./src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [RecursiveActor](./src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) | [HybridActorStructural](./src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) | [StructuralConcurrency](./src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) |
| **Performance (JMH)** | **482.59 ms/op** ⭐ | 464.61 ms/op | **500.36 ms/op** | 495.82 ms/op | 514.10 ms/op | 730.20 ms/op | 709.23 ms/op | 1709.19 ms/op ⚠️ | 1518.68 ms/op ⚠️ | 1816.83 ms/op ⚠️ |
| **Throughput** | **Highest** | **Highest** | **Highest** | **Highest** | **Highest** | High | High | Low | Low | Low |
| **Performance Rank** | **1st** | 2nd | 3rd | 4th | 5th | 6th | 7th | 8th | 9th | 10th |
| **Threading** | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (configurable) | Single-threaded | Single-threaded | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) |
| **Complexity** | Very Complex | Medium | Very Complex | Complex | Very Complex | Medium | Simple | Very Complex | Complex | Medium |
| **Order** | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Scales with cores/threads | Scales with virtual threads | Scales with virtual threads + actors | Scales with cores/threads | Scales with actors (distributed) | Limited | Limited | Scales with actors (dynamic) | Scales with virtual threads + actors | Scales with virtual threads |
| **Stack Safety** | N/A | Yes (structured scopes) | Yes (async recursion) | N/A | N/A | Yes (trampoline) | N/A | Yes (async recursion) | Yes (structured scopes) | Yes (structured scopes) |
| **Fault Tolerance** | Limited | High | High | Limited | High | None | None | High | Very High | High |
| **Performance Stability** | **Excellent** | Good | Good | **Excellent** | Good | Good | Good | Poor | Fair | Fair |


## Conclusion

All crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements, with performance being a critical factor based on JMH benchmark results:

### 🏆 **Performance Leaders (Based on Latest JMH Results - 2025-01-09)**
- **Choose Multi-threaded Iterative (v4)** for **maximum performance** (482.59 ms/op) with efficient iterative processing and excellent stability
- **Choose Structured Worker (v9)** for **near-optimal performance** (464.61 ms/op) with modern Java 25 structured concurrency and virtual threads
- **Choose Virtual Thread Actor (v10)** for **excellent performance** (500.36 ms/op) with simplified async processing and virtual thread-based actors

### ⚖️ **Balanced Options**
- **Choose Producer-Consumer (v2)** for solid performance (495.82 ms/op) with clear separation of concerns and proven scalability
- **Choose Actor (v5)** for solid performance (514.10 ms/op) with fault-tolerant, distributed systems and message-driven concurrency
- **Choose Recursive (v3)** for solid performance (730.20 ms/op) with elegant functional programming and deep recursion without stack overflow

### ⚠️ **Performance Concerns**
- **Avoid Recursive Actor (v6)** - Poor performance (1709.19 ms/op - 3.5x slower than best) with high variance
- **Avoid Hybrid Actor-Structural (v8)** - Poor performance (1518.68 ms/op - 3.1x slower than best) with significant overhead
- **Avoid Structural Concurrency (v7)** - Poor performance (1816.83 ms/op - 3.8x slower than best) with significant overhead

### 🆕 **New Implementation**
- **Virtual Thread Actor (v10)** - Successfully benchmarked with excellent performance (500.36 ms/op - 3rd place). Delivers on its promise of simplified async processing with virtual threads and better resource utilization compared to v5. Uses virtual thread-based actors for lightweight concurrency with supervisor pattern for fault tolerance.

### 📊 **Key Performance Insights**
1. **Multi-threaded iterative approach** (v4) leads performance with traditional Java concurrency patterns
2. **Virtual threads** (v9, v10) deliver excellent performance, with v9 achieving 2nd place and v10 achieving 3rd place
3. **Traditional Java concurrency** (ThreadPoolExecutor, BlockingQueue) continues to excel in performance and stability
4. **Actor model** (v5) remains competitive with fault-tolerant, distributed systems
5. **Recursive Actor approaches** (v6) show significant performance degradation in recent benchmarks
6. **Hybrid approaches** (v7, v8) introduce complexity without performance benefits
7. **Virtual Thread Actor (v10)** successfully delivers on its promise of simplified async processing with excellent performance

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
