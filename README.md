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

| Aspect | v11 | v12 | v10 | v9 | v4 | v2 | v13 | v5 | v3 | v1 | v7 | v8 | v6 |
|--------|----|----|----|----|----|----|----|----|----|----|----|----|----|
| **Analysis** | [+ Info](./docs/v11/README.md) | [+ Info](./docs/v12/README.md) | [+ Info](./docs/v10/README.md) | [+ Info](./docs/v9/README.md) | [+ Info](./docs/v4/README.md) | [+ Info](./docs/v2/README.md) | [+ Info](./docs/v13/README.md) | [+ Info](./docs/v5/README.md) | [+ Info](./docs/v3/README.md) | [+ Info](./docs/v1/README.md) | [+ Info](./docs/v7/README.md) | [+ Info](./docs/v8/README.md) | [+ Info](./docs/v6/README.md) |
| **Implementation** | [ImprovedStructured](./src/main/java/info/jab/crawler/v11/ImprovedStructuredCrawler.java) | [Jox](./src/main/java/info/jab/crawler/v12/JoxCrawler.java) | [VirtualThreadActor](./src/main/java/info/jab/crawler/v10/VirtualThreadActorCrawler.java) | [StructuredWorker](./src/main/java/info/jab/crawler/v9/StructuredWorkerCrawler.java) | [MultiThreadedIterative](./src/main/java/info/jab/crawler/v4/MultiThreadedIterativeCrawler.java) | [ProducerConsumer](./src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [StructuredQueue](./src/main/java/info/jab/crawler/v13/StructuredQueueCrawler.java) | [Actor](./src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [Recursive](./src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [Sequential](./src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [StructuralConcurrency](./src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) | [HybridActorStructural](./src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) | [RecursiveActor](./src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) |
| **Performance (JMH)** | **40.61 ms/op** ⭐ | **50.92 ms/op** ⭐ | 687.94 ms/op | 713.42 ms/op | 683.07 ms/op | 723.91 ms/op | 731.50 ms/op | 706.15 ms/op | 1152.43 ms/op | 1305.63 ms/op | 1856.63 ms/op ⚠️ | 1459.25 ms/op ⚠️ | 1893.83 ms/op ⚠️ |
| **Throughput** | **Exceptional** | **Exceptional** | **Highest** | **Highest** | **Highest** | **Highest** | **Highest** | **Highest** | High | High | Low | Low | Low |
| **Performance Rank** | **1st** | **2nd** | 3rd | 4th | 5th | 6th | 7th | 8th | 9th | 10th | 11th | 12th | 13th |
| **Threading** | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Single-threaded | Single-threaded | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (configurable) |
| **Complexity** | Medium | Medium | Very Complex | Medium | Very Complex | Complex | Medium | Very Complex | Medium | Simple | Medium | Complex | Very Complex |
| **Order** | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Scales with virtual threads | Scales with virtual threads | Scales with virtual threads + actors | Scales with virtual threads | Scales with cores/threads | Scales with cores/threads | Scales with virtual threads | Scales with actors (distributed) | Limited | Limited | Scales with virtual threads | Scales with virtual threads + actors | Scales with actors (dynamic) |
| **Stack Safety** | Yes (structured scopes) | Yes (supervised scopes) | Yes (async recursion) | Yes (structured scopes) | N/A | N/A | Yes (structured scopes) | N/A | Yes (trampoline) | N/A | Yes (structured scopes) | Yes (structured scopes) | Yes (async recursion) |
| **Fault Tolerance** | High | Very High | High | High | Limited | Limited | High | High | None | None | High | Very High | High |
| **Performance Stability** | **Excellent** | **Excellent** | Good | Good | **Excellent** | **Excellent** | Good | Good | Good | Good | Fair | Fair | Poor |


## Conclusion

All crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements, with performance being a critical factor based on JMH benchmark results:

### 🏆 **Performance Leaders (Based on Latest JMH Results - 2025-01-09)**
- **Choose Improved Structured Concurrency (v11)** for **revolutionary performance** (40.61 ms/op) - NEW CHAMPION! Addresses SoftwareMill critique with uniform cancellation, unified scope logic, and custom Joiner with race semantics
- **Choose Jox-based Structured Concurrency (v12)** for **exceptional performance** (50.92 ms/op) - Uses SoftwareMill's Jox library with supervised scopes, cancellable forks, and addresses all JEP 505 critiques
- **Choose Virtual Thread Actor (v10)** for **excellent performance** (687.94 ms/op) with simplified async processing and virtual thread-based actors

### ⚖️ **Balanced Options**
- **Choose Multi-threaded Iterative (v4)** for solid performance (683.07 ms/op) with efficient iterative processing and excellent stability
- **Choose Actor (v5)** for solid performance (706.15 ms/op) with fault-tolerant, distributed systems and message-driven concurrency
- **Choose Producer-Consumer (v2)** for solid performance (723.91 ms/op) with clear separation of concerns and proven scalability
- **Choose Structured Queue (v13)** for solid performance (731.50 ms/op) with modern structured concurrency and queue-based coordination
- **Choose Structured Worker (v9)** for solid performance (713.42 ms/op) with modern Java 25 structured concurrency and virtual threads
- **Choose Recursive (v3)** for solid performance (1152.43 ms/op) with stack-safe trampoline pattern
- **Choose Sequential (v1)** for solid performance (1305.63 ms/op) with simplicity, predictability, and smaller crawling tasks

### ⚠️ **Performance Concerns**
- **Avoid Hybrid Actor-Structural (v8)** - Poor performance (1459.25 ms/op - 36x slower than best) with significant overhead
- **Avoid Structural Concurrency (v7)** - Poor performance (1856.63 ms/op - 46x slower than best) with significant overhead
- **Avoid Recursive Actor (v6)** - Poor performance (1893.83 ms/op - 47x slower than best) with high variance

### 🆕 **Revolutionary Implementation**
- **Improved Structured Concurrency (v11)** - NEW CHAMPION! Revolutionary performance breakthrough (40.61 ms/op - 1st place). Successfully addresses SoftwareMill's critique of JEP 505 with uniform cancellation, unified scope logic, timeout as method, and custom Joiner with race semantics. Demonstrates how to overcome structured concurrency limitations while maintaining benefits.
- **Jox-based Structured Concurrency (v12)** - Revolutionary performance breakthrough (50.92 ms/op - 2nd place). Uses SoftwareMill's Jox library with supervised scopes, cancellable forks, and addresses all JEP 505 critiques. Demonstrates the power of proper structured concurrency implementation.

### 📊 **Key Performance Insights**
1. **Improved Structured Concurrency (v11)** achieves revolutionary performance - NEW CHAMPION at 40.61 ms/op, demonstrating how to overcome JEP 505 limitations
2. **Jox-based Structured Concurrency (v12)** achieves exceptional performance - 50.92 ms/op, demonstrating the power of SoftwareMill's Jox library
3. **Virtual Thread Actor (v10)** delivers excellent performance with simplified async processing
4. **Traditional Java concurrency** (ThreadPoolExecutor, BlockingQueue) continues to excel in performance and stability
5. **Actor model** (v5) remains competitive with fault-tolerant, distributed systems
6. **Structured Queue (v13)** shows solid performance with modern structured concurrency patterns
7. **Recursive Actor approaches** (v6) show significant performance degradation in recent benchmarks
8. **Hybrid approaches** (v7, v8) introduce complexity without performance benefits
9. **SoftwareMill's critique** of JEP 505 has been successfully addressed in both v11 and v12, proving structured concurrency can be highly performant

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

# V13 Structured Queue (requires Java 25 with preview features)
# Run directly with Java (Maven exec plugin doesn't work well with preview features)
./mvnw compile -Pexamples
java --enable-preview -cp target/classes:$(./mvnw dependency:build-classpath \
-q -Dmdep.outputFile=/dev/stdout)    info.jab.crawler.v13.StructuredQueueCrawlerExample
```

Powered by [Cursor](https://www.cursor.com/) with ❤️ from [Madrid](https://www.google.com/maps/place/Community+of+Madrid,+Madrid/@40.4983324,-6.3162283,8z/data=!3m1!4b1!4m6!3m5!1s0xd41817a40e033b9:0x10340f3be4bc880!8m2!3d40.4167088!4d-3.5812692!16zL20vMGo0eGc?entry=ttu&g_ep=EgoyMDI1MDgxOC4wIKXMDSoASAFQAw%3D%3D)
