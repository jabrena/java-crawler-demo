# Web Crawler Architecture Overview

This document provides an overview of the eight different web crawler implementations in this project. Each approach solves the same problem—crawling web pages starting from a seed URL—but uses fundamentally different architectural patterns and concurrency models.

## Crawler Implementations

### [Sequential Crawler (v1)](./v1/README.md)
**Single-threaded, breadth-first traversal** pattern with simple, linear processing.

- **Threading**: Single-threaded
- **Throughput**: Low (one page at a time)
- **Complexity**: Simple
- **Use Case**: Small sites, prototyping

### [Producer-Consumer Crawler (v2)](./v2/README.md)
**Multi-threaded, parallel processing** pattern using worker threads and shared queues.

- **Threading**: Multi-threaded (configurable)
- **Throughput**: High (N pages simultaneously)
- **Complexity**: Complex
- **Use Case**: Large sites, production

### [Recursive Crawler (v3)](./v3/README.md)
**Single-threaded, functional programming** pattern using trampoline technique for stack-safe recursion.

- **Threading**: Single-threaded
- **Throughput**: Low (one page at a time)
- **Complexity**: Medium
- **Stack Safety**: Yes (trampoline)
- **Use Case**: Deep sites, functional programming

### [Multi-threaded Recursive Crawler (v4)](./v4/README.md)
**Hybrid approach** combining parallel processing with stack-safe recursion.

- **Threading**: Multi-threaded (configurable)
- **Throughput**: High (N pages simultaneously)
- **Complexity**: Very Complex
- **Stack Safety**: Yes (trampoline)
- **Use Case**: Large deep sites, maximum performance

### [Actor Crawler (v5)](./v5/README.md)
**Actor Model pattern** with message passing and supervisor-worker architecture.

- **Threading**: Multi-threaded (configurable)
- **Throughput**: High (N actors simultaneously)
- **Complexity**: Very Complex
- **Fault Tolerance**: High (supervisor pattern)
- **Use Case**: Distributed systems, fault-tolerant crawling

### [Recursive Actor Crawler (v6)](./v6/README.md)
**Hybrid actor model** with recursive design and dynamic actor spawning.

- **Threading**: Multi-threaded (configurable)
- **Throughput**: High (N actors simultaneously)
- **Complexity**: Very Complex
- **Stack Safety**: Yes (async recursion)
- **Fault Tolerance**: High (actor isolation)
- **Use Case**: Tree-like sites, dynamic scaling

### [Structural Concurrency Crawler (v7)](./v7/README.md)
**Java 25's structural concurrency** using StructuredTaskScope for automatic resource management.

- **Threading**: Multi-threaded (virtual threads)
- **Throughput**: High (virtual threads)
- **Complexity**: Medium
- **Stack Safety**: Yes (structured scopes)
- **Fault Tolerance**: High (scope isolation)
- **Use Case**: Modern Java, resource management

### [Hybrid Actor-Structural Concurrency Crawler (v8)](./v8/README.md)
**Hybrid approach** combining Actor Model coordination with structural concurrency execution.

- **Threading**: Multi-threaded (virtual threads)
- **Throughput**: High (virtual threads + actor coordination)
- **Complexity**: Complex
- **Stack Safety**: Yes (structured scopes)
- **Fault Tolerance**: Very High (actor + scope isolation)
- **Use Case**: Enterprise systems, maximum fault tolerance

## Comparison Matrix

| Aspect | v1 | v2 | v3 | v4 | v5 | v6 | v7 | v8 |
|--------|----|----|----|----|----|----|----|----|
| **Threading** | Single-threaded | Multi-threaded (configurable) | Single-threaded | Multi-threaded (configurable) | Multi-threaded (configurable) | Multi-threaded (configurable) | Multi-threaded (virtual threads) | Multi-threaded (virtual threads) |
| **Throughput** | Low | High | Low | High | High | High | High | High |
| **Complexity** | Simple | Complex | Medium | Very Complex | Very Complex | Very Complex | Medium | Complex |
| **Resource Usage** | Low | Higher | Low | Higher | Higher | Higher | Low | Medium |
| **Order** | Deterministic | Non-deterministic | Deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic | Non-deterministic |
| **Scalability** | Limited | Scales with cores/threads | Limited | Scales with cores/threads | Scales with actors (distributed) | Scales with actors (dynamic) | Scales with virtual threads | Scales with virtual threads + actors |
| **Debugging** | Easy | More challenging | Medium | Most challenging | Most challenging | Most challenging | Medium | Challenging |
| **Stack Safety** | N/A | N/A | Yes (trampoline) | Yes (trampoline) | N/A | Yes (async recursion) | Yes (structured scopes) | Yes (structured scopes) |
| **Programming Style** | Imperative | Imperative | Functional | Hybrid | Actor Model | Hybrid Actor-Recursive | Structured Concurrency | Hybrid Actor-Structural |
| **Fault Tolerance** | None | Limited | None | Limited | High | High | High | Very High |

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

## Conclusion

All eight crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements:

- **Choose Sequential (v1)** for simplicity, predictability, and smaller crawling tasks
- **Choose Producer-Consumer (v2)** for performance, scalability, and large-scale crawling operations
- **Choose Recursive (v3)** for elegant functional programming and deep recursion without stack overflow
- **Choose Multi-threaded Recursive (v4)** for maximum performance with stack-safe deep recursion
- **Choose Actor (v5)** for fault-tolerant, distributed systems and message-driven concurrency
- **Choose Recursive Actor (v6)** for tree-like sites with dynamic actor spawning and natural web topology matching
- **Choose Structural Concurrency (v7)** for modern Java applications requiring automatic resource management and virtual thread efficiency
- **Choose Hybrid Actor-Structural (v8)** for enterprise systems requiring maximum fault tolerance with both actor coordination and automatic resource management

Each implementation offers unique tradeoffs between simplicity, performance, fault tolerance, and programming paradigm, showcasing different approaches to solving complex concurrent programming challenges. The evolution from v1 to v8 demonstrates the progression of Java concurrency features and design patterns, from simple sequential processing to modern hybrid architectures combining actor models with structured concurrency for enterprise-grade fault tolerance.
