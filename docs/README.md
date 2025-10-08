# Web Crawler Architecture Overview

This document provides an overview of the eight different web crawler implementations in this project. Each approach solves the same problem—crawling web pages starting from a seed URL—but uses fundamentally different architectural patterns and concurrency models.

## Comparison Matrix

| Aspect | v1 | v2 | v3 | v4 | v5 | v6 | v7 | v8 |
|--------|----|----|----|----|----|----|----|----|
| **Analysis** | [More](./v1/README.md) | [More](./v2/README.md) | [More](./v3/README.md) | [More](./v4/README.md) | [More](./v5/README.md) | [More](./v6/README.md) | [More](./v7/README.md) | [More](./v8/README.md) |
| **Implementation** | [SequentialCrawler](../src/main/java/info/jab/crawler/v1/SequentialCrawler.java) | [ProducerConsumerCrawler](../src/main/java/info/jab/crawler/v2/ProducerConsumerCrawler.java) | [RecursiveCrawler](../src/main/java/info/jab/crawler/v3/RecursiveCrawler.java) | [MultiThreadedRecursiveCrawler](../src/main/java/info/jab/crawler/v4/MultiThreadedRecursiveCrawler.java) | [ActorCrawler](../src/main/java/info/jab/crawler/v5/ActorCrawler.java) | [RecursiveActorCrawler](../src/main/java/info/jab/crawler/v6/RecursiveActorCrawler.java) | [StructuralConcurrencyCrawler](../src/main/java/info/jab/crawler/v7/StructuralConcurrencyCrawler.java) | [HybridActorStructuralCrawler](../src/main/java/info/jab/crawler/v8/HybridActorStructuralCrawler.java) |
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

### 🎯 **Production Recommendations**
- **Primary Choice**: Multi-threaded Recursive (v4) - Best performance, stability, and proven patterns
- **Alternative**: Producer-Consumer (v2) - Excellent performance with cleaner architecture
- **Special Cases**: Actor (v5) for distributed systems requiring fault tolerance

Each implementation offers unique tradeoffs between simplicity, performance, fault tolerance, and programming paradigm. The JMH results reveal that **performance doesn't always correlate with architectural complexity** - the most complex implementations (v6, v7, v8) actually perform worse than simpler alternatives. This demonstrates the importance of empirical performance testing when choosing architectural patterns for production systems.

## Architecture Evolution

The project demonstrates how the same problem can be solved with different architectural patterns and concurrency models:

- **v1**: Simple iterative approach with queue-based BFS
- **v2**: Multi-threaded producer-consumer pattern for parallel processing
- **v3**: Functional programming with trampoline pattern for stack-safe recursion
- **v4**: Hybrid approach combining parallel processing with stack-safe recursion
- **v5**: Actor model with message passing for fault-tolerant distributed systems
- **v6**: Hybrid actor-recursive model with dynamic spawning for natural tree structures
- **v7**: Java 25's structural concurrency with automatic resource management and virtual threads
- **v8**: Hybrid actor-structural concurrency combining actor coordination with automatic resource management for maximum fault tolerance
