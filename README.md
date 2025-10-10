# Java Crawler Demo

A repository about a collection of web crawler implementation in Java.

## Comparison Matrix

*Performance data based on JMH benchmarks (2025-01-10) using Java 25 with GraalVM CE, 1 fork, 5 measurement iterations*

**Benchmark Environment:**
- **OS**: macOS (Darwin 23.5.0)
- **Hardware**: Apple M1 Chip, 8 cores (4 performance + 4 efficiency), 16 GB RAM
- **Runtime**: Docker container with GraalVM CE Java 25
- **JVM Args**: `-Xms1G -Xmx2G --enable-preview`
- **Measurement**: Average time per operation (avgt mode)

| Aspect | v8 | v1 | v3 | v7 | v11 | v12 | v6 | v10 | v5 | v4 | v2 | v9 | v13 |
|--------|----|----|----|----|----|----|----|----|----|----|----|----|----|
| **Analysis** | [+ Info](./docs/v8/README.md) | [+ Info](./docs/v1/README.md) | [+ Info](./docs/v3/README.md) | [+ Info](./docs/v7/README.md) | [+ Info](./docs/v11/README.md) | [+ Info](./docs/v12/README.md) | [+ Info](./docs/v6/README.md) | [+ Info](./docs/v10/README.md) | [+ Info](./docs/v5/README.md) | [+ Info](./docs/v4/README.md) | [+ Info](./docs/v2/README.md) | [+ Info](./docs/v9/README.md) | [+ Info](./docs/v13/README.md) |
| **Implementation** | [HybridActorStructural](./src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) | [Sequential](./src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [Recursive](./src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [StructuralConcurrency](./src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) | [ImprovedStructured](./src/main/java/info/jab/crawler/v11/ImprovedStructuredCrawler.java) | [Jox](./src/main/java/info/jab/crawler/v12/JoxCrawler.java) | [RecursiveActor](./src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) | [VirtualThreadActor](./src/main/java/info/jab/crawler/v10/VirtualThreadActorCrawler.java) | [Actor](./src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [MultiThreadedIterative](./src/main/java/info/jab/crawler/v4/MultiThreadedIterativeCrawler.java) | [ProducerConsumer](./src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [StructuredWorker](./src/main/java/info/jab/crawler/v9/StructuredWorkerCrawler.java) | [StructuredQueue](./src/main/java/info/jab/crawler/v13/StructuredQueueCrawler.java) |
| **Performance (JMH)** | **0.347 ms/op** ⭐ | **0.349 ms/op** ⭐ | **0.350 ms/op** ⭐ | **0.439 ms/op** ⭐ | **0.465 ms/op** ⭐ | **0.494 ms/op** ⭐ | **0.563 ms/op** ⭐ | 11.901 ms/op | 12.190 ms/op | 104.448 ms/op | 105.045 ms/op | 108.247 ms/op | 108.971 ms/op |
| **Throughput** | **Exceptional** | **Exceptional** | **Exceptional** | **Exceptional** | **Exceptional** | **Exceptional** | **Exceptional** | **High** | **High** | **Medium** | **Medium** | **Medium** | **Medium** |
| **Performance Rank** | **1st** | **2nd** | **3rd** | **4th** | **5th** | **6th** | **7th** | **8th** | **9th** | **10th** | **11th** | **12th** | **13th** |
| **Threading** | Multi-threaded (virtual threads) | Single-threaded | Single-threaded | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (configurable) | Multi-threaded (configurable) | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) |
| **Complexity** | Complex | Simple | Medium | Medium | Medium | Medium | Very Complex | Very Complex | Very Complex | Very Complex | Complex | Medium | Medium |
| **Order** | Non-deterministic | Deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Scales with virtual threads + actors | Limited | Limited | Scales with virtual threads | Scales with virtual threads | Scales with virtual threads | Scales with actors (dynamic) | Scales with virtual threads + actors | Scales with actors (distributed) | Scales with cores/threads | Scales with cores/threads | Scales with virtual threads | Scales with virtual threads |
| **Stack Safety** | Yes (structured scopes) | N/A | Yes (trampoline) | Yes (structured scopes) | Yes (structured scopes) | Yes (supervised scopes) | Yes (async recursion) | Yes (async recursion) | N/A | N/A | N/A | Yes (structured scopes) | Yes (structured scopes) |
| **Fault Tolerance** | Very High | None | None | High | High | Very High | High | High | High | Limited | Limited | High | High |
| **Performance Stability** | Fair | Good | Good | Fair | **Excellent** | **Excellent** | Poor | Good | Good | **Excellent** | **Excellent** | Good | Good |


## Conclusion

All crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements, with performance being a critical factor based on JMH benchmark results:

### 🏆 **Performance Leaders (Based on Latest JMH Results - 2025-01-10)**
- **Choose Hybrid Actor-Structural (v8)** for **revolutionary performance** (0.347 ms/op) - NEW CHAMPION! Combines virtual threads with structured concurrency for optimal performance
- **Choose Sequential (v1)** for **exceptional performance** (0.349 ms/op) - Simple, predictable, and extremely fast for smaller crawling tasks
- **Choose Recursive (v3)** for **exceptional performance** (0.350 ms/op) - Stack-safe trampoline pattern with excellent performance

### ⚖️ **Balanced Options**
- **Choose Structural Concurrency (v7)** for solid performance (0.439 ms/op) with modern Java 25 structured concurrency
- **Choose Improved Structured Concurrency (v11)** for solid performance (0.465 ms/op) with uniform cancellation and unified scope logic
- **Choose Jox-based Structured Concurrency (v12)** for solid performance (0.494 ms/op) with SoftwareMill's Jox library
- **Choose Recursive Actor (v6)** for solid performance (0.563 ms/op) with async recursion and actor model
- **Choose Virtual Thread Actor (v10)** for solid performance (11.901 ms/op) with simplified async processing
- **Choose Actor (v5)** for solid performance (12.190 ms/op) with fault-tolerant, distributed systems
- **Choose Multi-threaded Iterative (v4)** for solid performance (104.448 ms/op) with efficient iterative processing
- **Choose Producer-Consumer (v2)** for solid performance (105.045 ms/op) with clear separation of concerns
- **Choose Structured Worker (v9)** for solid performance (108.247 ms/op) with modern Java 25 structured concurrency
- **Choose Structured Queue (v13)** for solid performance (108.971 ms/op) with modern structured concurrency

### 🆕 **Revolutionary Implementation**
- **Hybrid Actor-Structural (v8)** - NEW CHAMPION! Revolutionary performance breakthrough (0.347 ms/op - 1st place). Successfully combines virtual threads with structured concurrency for optimal performance.
- **Sequential (v1)** - Exceptional performance breakthrough (0.349 ms/op - 2nd place). Demonstrates that simplicity and predictability can achieve exceptional performance for smaller crawling tasks.
- **Recursive (v3)** - Exceptional performance breakthrough (0.350 ms/op - 3rd place). Stack-safe trampoline pattern proves that recursive approaches can be highly performant.

### 📊 **Key Performance Insights**
1. **Hybrid Actor-Structural (v8)** achieves revolutionary performance - NEW CHAMPION at 0.347 ms/op, successfully combining virtual threads with structured concurrency
2. **Sequential (v1)** achieves exceptional performance - 0.349 ms/op, demonstrating that simplicity can be extremely effective
3. **Recursive (v3)** achieves exceptional performance - 0.350 ms/op, proving stack-safe trampoline patterns are highly performant
4. **All structured concurrency approaches** (v7, v11, v12) now show excellent performance under 1ms/op
5. **Actor model approaches** (v5, v6, v10) demonstrate solid performance with fault-tolerant, distributed systems
6. **Traditional Java concurrency** (ThreadPoolExecutor, BlockingQueue) continues to excel in performance and stability
7. **Virtual threads** are proving to be highly effective across multiple implementations
8. **Performance gap** between best and worst implementations has significantly narrowed
9. **Modern Java concurrency features** (structured concurrency, virtual threads) are delivering exceptional results

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
