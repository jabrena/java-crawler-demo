# Java Crawler Demo

A repository about a collection of web crawler implementation in Java.

## Comparison Matrix

*Performance data based on JMH benchmarks (2025-01-09) using Java 25 with GraalVM CE, 2 forks, 5 measurement iterations*

| Aspect | v11 | v5 | v10 | v4 | v9 | v2 | v3 | v1 | v6 | v8 | v7 |
|--------|----|----|----|----|----|----|----|----|----|----|----|
| **Analysis** | [+ Info](./docs/v11/README.md) | [+ Info](./docs/v5/README.md) | [+ Info](./docs/v10/README.md) | [+ Info](./docs/v4/README.md) | [+ Info](./docs/v9/README.md) | [+ Info](./docs/v2/README.md) | [+ Info](./docs/v3/README.md) | [+ Info](./docs/v1/README.md) | [+ Info](./docs/v6/README.md) | [+ Info](./docs/v8/README.md) | [+ Info](./docs/v7/README.md) |
| **Implementation** | [ImprovedStructured](./src/main/java/info/jab/crawler/v11/ImprovedStructuredCrawler.java) | [Actor](./src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [VirtualThreadActor](./src/main/java/info/jab/crawler/v10/VirtualThreadActorCrawler.java) | [MultiThreadedIterative](./src/main/java/info/jab/crawler/v4/MultiThreadedIterativeCrawler.java) | [StructuredWorker](./src/main/java/info/jab/crawler/v9/StructuredWorkerCrawler.java) | [ProducerConsumer](./src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [Recursive](./src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [Sequential](./src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [RecursiveActor](./src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) | [HybridActorStructural](./src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) | [StructuralConcurrency](./src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) |
| **Performance (JMH)** | **27.98 ms/op** ⭐ | 454.15 ms/op | 506.36 ms/op | 553.36 ms/op | 582.83 ms/op | 612.00 ms/op | 694.29 ms/op | 729.40 ms/op | 1388.80 ms/op ⚠️ | 1592.85 ms/op ⚠️ | 1818.97 ms/op ⚠️ |
| **Throughput** | **Exceptional** | **Highest** | **Highest** | **Highest** | **Highest** | **Highest** | High | High | Low | Low | Low |
| **Performance Rank** | **1st** | 2nd | 3rd | 4th | 5th | 6th | 7th | 8th | 9th | 10th | 11th |
| **Threading** | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Single-threaded | Single-threaded | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) |
| **Complexity** | Medium | Very Complex | Very Complex | Very Complex | Medium | Complex | Medium | Simple | Very Complex | Complex | Medium |
| **Order** | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Scales with virtual threads | Scales with actors (distributed) | Scales with virtual threads + actors | Scales with cores/threads | Scales with virtual threads | Scales with cores/threads | Limited | Limited | Scales with actors (dynamic) | Scales with virtual threads + actors | Scales with virtual threads |
| **Stack Safety** | Yes (structured scopes) | N/A | Yes (async recursion) | N/A | Yes (structured scopes) | N/A | Yes (trampoline) | N/A | Yes (async recursion) | Yes (structured scopes) | Yes (structured scopes) |
| **Fault Tolerance** | High | High | High | Limited | High | Limited | None | None | High | Very High | High |
| **Performance Stability** | **Excellent** | Good | Good | **Excellent** | Good | **Excellent** | Good | Good | Poor | Fair | Fair |


## Conclusion

All crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements, with performance being a critical factor based on JMH benchmark results:

### 🏆 **Performance Leaders (Based on Latest JMH Results - 2025-01-09)**
- **Choose Improved Structured Concurrency (v11)** for **exceptional performance** (27.98 ms/op) - 16x faster than previous best! Addresses SoftwareMill critique with uniform cancellation, unified scope logic, and custom Joiner with race semantics
- **Choose Actor (v5)** for **excellent performance** (454.15 ms/op) with fault-tolerant, distributed systems and message-driven concurrency
- **Choose Virtual Thread Actor (v10)** for **excellent performance** (506.36 ms/op) with simplified async processing and virtual thread-based actors

### ⚖️ **Balanced Options**
- **Choose Multi-threaded Iterative (v4)** for solid performance (553.36 ms/op) with efficient iterative processing and excellent stability
- **Choose Structured Worker (v9)** for solid performance (582.83 ms/op) with modern Java 25 structured concurrency and virtual threads
- **Choose Producer-Consumer (v2)** for solid performance (612.00 ms/op) with clear separation of concerns and proven scalability
- **Choose Recursive (v3)** for solid performance (694.29 ms/op) with stack-safe trampoline pattern
- **Choose Sequential (v1)** for solid performance (729.40 ms/op) with simplicity, predictability, and smaller crawling tasks

### ⚠️ **Performance Concerns**
- **Avoid Structural Concurrency (v7)** - Poor performance (1818.97 ms/op - 65x slower than best) with significant overhead
- **Avoid Hybrid Actor-Structural (v8)** - Poor performance (1592.85 ms/op - 57x slower than best) with significant overhead
- **Avoid Recursive Actor (v6)** - Poor performance (1388.80 ms/op - 50x slower than best) with high variance

### 🆕 **Revolutionary Implementation**
- **Improved Structured Concurrency (v11)** - Revolutionary performance breakthrough (27.98 ms/op - 1st place by a huge margin). Successfully addresses SoftwareMill's critique of JEP 505 with uniform cancellation, unified scope logic, timeout as method, and custom Joiner with race semantics. Demonstrates how to overcome structured concurrency limitations while maintaining benefits.

### 📊 **Key Performance Insights**
1. **Improved Structured Concurrency (v11)** achieves revolutionary performance - 16x faster than previous best implementations
2. **Actor model** (v5) remains highly competitive with fault-tolerant, distributed systems
3. **Virtual Thread Actor (v10)** delivers excellent performance with simplified async processing
4. **Traditional Java concurrency** (ThreadPoolExecutor, BlockingQueue) continues to excel in performance and stability
5. **Recursive Actor approaches** (v6) show significant performance degradation in recent benchmarks
6. **Hybrid approaches** (v7, v8) introduce complexity without performance benefits
7. **SoftwareMill's critique** of JEP 505 has been successfully addressed in v11, proving structured concurrency can be highly performant

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
# V11 Improved Structured Concurrency (requires Java 25 with preview features)
# Run directly with Java (Maven exec plugin doesn't work well with preview features)
./mvnw compile -Pexamples
java --enable-preview -cp target/classes:$(./mvnw dependency:build-classpath \
-q -Dmdep.outputFile=/dev/stdout)    info.jab.crawler.v11.ImprovedStructuredCrawlerExample
```

Powered by [Cursor](https://www.cursor.com/) with ❤️ from [Madrid](https://www.google.com/maps/place/Community+of+Madrid,+Madrid/@40.4983324,-6.3162283,8z/data=!3m1!4b1!4m6!3m5!1s0xd41817a40e033b9:0x10340f3be4bc880!8m2!3d40.4167088!4d-3.5812692!16zL20vMGo0eGc?entry=ttu&g_ep=EgoyMDI1MDgxOC4wIKXMDSoASAFQAw%3D%3D)
