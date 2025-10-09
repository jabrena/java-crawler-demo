# Java Crawler Demo

A repository about a collection of web crawler implementation in Java.

## Comparison Matrix

*Performance data based on JMH benchmarks (2025-01-09) using Java 25 with GraalVM CE, 2 forks, 5 measurement iterations*

**Benchmark Environment:**
- **OS**: macOS (Darwin 23.5.0)
- **Hardware**: Apple M1 Chip, 8 cores (4 performance + 4 efficiency), 16 GB RAM
- **Runtime**: Docker container with GraalVM CE Java 25
- **JVM Args**: `-Xms1G -Xmx2G --enable-preview`
- **Measurement**: Average time per operation (avgt mode)

| Aspect | v12 | v11 | v10 | v9 | v4 | v2 | v3 | v1 | v5 | v7 | v8 | v6 |
|--------|----|----|----|----|----|----|----|----|----|----|----|----|
| **Analysis** | [+ Info](./docs/v12/README.md) | [+ Info](./docs/v11/README.md) | [+ Info](./docs/v10/README.md) | [+ Info](./docs/v9/README.md) | [+ Info](./docs/v4/README.md) | [+ Info](./docs/v2/README.md) | [+ Info](./docs/v3/README.md) | [+ Info](./docs/v1/README.md) | [+ Info](./docs/v5/README.md) | [+ Info](./docs/v7/README.md) | [+ Info](./docs/v8/README.md) | [+ Info](./docs/v6/README.md) |
| **Implementation** | [Jox](./src/main/java/info/jab/crawler/v12/JoxCrawler.java) | [ImprovedStructured](./src/main/java/info/jab/crawler/v11/ImprovedStructuredCrawler.java) | [VirtualThreadActor](./src/main/java/info/jab/crawler/v10/VirtualThreadActorCrawler.java) | [StructuredWorker](./src/main/java/info/jab/crawler/v9/StructuredWorkerCrawler.java) | [MultiThreadedIterative](./src/main/java/info/jab/crawler/v4/MultiThreadedIterativeCrawler.java) | [ProducerConsumer](./src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [Recursive](./src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [Sequential](./src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [Actor](./src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [StructuralConcurrency](./src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) | [HybridActorStructural](./src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) | [RecursiveActor](./src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) |
| **Performance (JMH)** | **45.80 ms/op** ⭐ | 49.08 ms/op ⭐ | 589.26 ms/op | 606.51 ms/op | 626.41 ms/op | 640.56 ms/op | 1001.69 ms/op | 1067.57 ms/op | 866.45 ms/op | 1652.36 ms/op ⚠️ | 1869.91 ms/op ⚠️ | 1936.21 ms/op ⚠️ |
| **Throughput** | **Exceptional** | **Exceptional** | **Highest** | **Highest** | **Highest** | **Highest** | High | High | **Highest** | Low | Low | Low |
| **Performance Rank** | **1st** | **2nd** | 3rd | 4th | 5th | 6th | 7th | 8th | 9th | 10th | 11th | 12th |
| **Threading** | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (configurable) | Single-threaded | Single-threaded | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (configurable) |
| **Complexity** | Medium | Medium | Very Complex | Medium | Very Complex | Complex | Medium | Simple | Very Complex | Medium | Complex | Very Complex |
| **Order** | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Scales with virtual threads | Scales with virtual threads | Scales with virtual threads + actors | Scales with virtual threads | Scales with cores/threads | Scales with cores/threads | Limited | Limited | Scales with actors (distributed) | Scales with virtual threads | Scales with virtual threads + actors | Scales with actors (dynamic) |
| **Stack Safety** | Yes (supervised scopes) | Yes (structured scopes) | Yes (async recursion) | Yes (structured scopes) | N/A | N/A | Yes (trampoline) | N/A | N/A | Yes (structured scopes) | Yes (structured scopes) | Yes (async recursion) |
| **Fault Tolerance** | Very High | High | High | High | Limited | Limited | None | None | High | High | Very High | High |
| **Performance Stability** | **Excellent** | **Excellent** | Good | Good | **Excellent** | **Excellent** | Good | Good | Good | Fair | Fair | Poor |


## Conclusion

All crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements, with performance being a critical factor based on JMH benchmark results:

### 🏆 **Performance Leaders (Based on Latest JMH Results - 2025-01-09)**
- **Choose Jox-based Structured Concurrency (v12)** for **revolutionary performance** (45.80 ms/op) - NEW CHAMPION! Uses SoftwareMill's Jox library with supervised scopes, cancellable forks, and addresses all JEP 505 critiques
- **Choose Improved Structured Concurrency (v11)** for **exceptional performance** (49.08 ms/op) - Addresses SoftwareMill critique with uniform cancellation, unified scope logic, and custom Joiner with race semantics
- **Choose Virtual Thread Actor (v10)** for **excellent performance** (589.26 ms/op) with simplified async processing and virtual thread-based actors

### ⚖️ **Balanced Options**
- **Choose Structured Worker (v9)** for solid performance (606.51 ms/op) with modern Java 25 structured concurrency and virtual threads
- **Choose Multi-threaded Iterative (v4)** for solid performance (626.41 ms/op) with efficient iterative processing and excellent stability
- **Choose Producer-Consumer (v2)** for solid performance (640.56 ms/op) with clear separation of concerns and proven scalability
- **Choose Actor (v5)** for solid performance (866.45 ms/op) with fault-tolerant, distributed systems and message-driven concurrency
- **Choose Recursive (v3)** for solid performance (1001.69 ms/op) with stack-safe trampoline pattern
- **Choose Sequential (v1)** for solid performance (1067.57 ms/op) with simplicity, predictability, and smaller crawling tasks

### ⚠️ **Performance Concerns**
- **Avoid Structural Concurrency (v7)** - Poor performance (1652.36 ms/op - 36x slower than best) with significant overhead
- **Avoid Hybrid Actor-Structural (v8)** - Poor performance (1869.91 ms/op - 41x slower than best) with significant overhead
- **Avoid Recursive Actor (v6)** - Poor performance (1936.21 ms/op - 42x slower than best) with high variance

### 🆕 **Revolutionary Implementation**
- **Jox-based Structured Concurrency (v12)** - NEW CHAMPION! Revolutionary performance breakthrough (45.80 ms/op - 1st place). Uses SoftwareMill's Jox library with supervised scopes, cancellable forks, and addresses all JEP 505 critiques. Demonstrates the power of proper structured concurrency implementation.
- **Improved Structured Concurrency (v11)** - Revolutionary performance breakthrough (49.08 ms/op - 2nd place). Successfully addresses SoftwareMill's critique of JEP 505 with uniform cancellation, unified scope logic, timeout as method, and custom Joiner with race semantics. Demonstrates how to overcome structured concurrency limitations while maintaining benefits.

### 📊 **Key Performance Insights**
1. **Jox-based Structured Concurrency (v12)** achieves revolutionary performance - NEW CHAMPION at 45.80 ms/op, demonstrating the power of SoftwareMill's Jox library
2. **Improved Structured Concurrency (v11)** achieves exceptional performance - 49.08 ms/op, 13x faster than traditional approaches
3. **Virtual Thread Actor (v10)** delivers excellent performance with simplified async processing
4. **Traditional Java concurrency** (ThreadPoolExecutor, BlockingQueue) continues to excel in performance and stability
5. **Actor model** (v5) remains competitive with fault-tolerant, distributed systems
6. **Recursive Actor approaches** (v6) show significant performance degradation in recent benchmarks
7. **Hybrid approaches** (v7, v8) introduce complexity without performance benefits
8. **SoftwareMill's critique** of JEP 505 has been successfully addressed in both v11 and v12, proving structured concurrency can be highly performant

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

# V12 Jox-based Structured Concurrency (requires Java 25 with preview features)
# Run directly with Java (Maven exec plugin doesn't work well with preview features)
./mvnw compile -Pexamples
java --enable-preview -cp target/classes:$(./mvnw dependency:build-classpath \
-q -Dmdep.outputFile=/dev/stdout)    info.jab.crawler.v12.JoxCrawlerExample
```

Powered by [Cursor](https://www.cursor.com/) with ❤️ from [Madrid](https://www.google.com/maps/place/Community+of+Madrid,+Madrid/@40.4983324,-6.3162283,8z/data=!3m1!4b1!4m6!3m5!1s0xd41817a40e033b9:0x10340f3be4bc880!8m2!3d40.4167088!4d-3.5812692!16zL20vMGo0eGc?entry=ttu&g_ep=EgoyMDI1MDgxOC4wIKXMDSoASAFQAw%3D%3D)
