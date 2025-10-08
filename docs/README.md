# Web Crawler Architecture Overview

This document explains the four different web crawler implementations in this project: the Sequential Crawler (v1), Producer-Consumer Crawler (v2), Recursive Crawler (v3), and Multi-threaded Recursive Crawler (v4). All approaches solve the same problem—crawling web pages starting from a seed URL—but use fundamentally different architectural patterns and concurrency models.

## Sequential Crawler (v1)

### Core Concept

The Sequential Crawler implements a **single-threaded, breadth-first traversal** pattern. It processes web pages one at a time in a simple, linear fashion.

### How It Works

1. **Initialization**: The client creates a crawler using the builder pattern, specifying constraints like maximum depth and maximum pages.

2. **Processing Loop**:
   - Starting with the seed URL, the crawler maintains a queue of URLs to visit
   - For each URL in the queue:
     - Fetch the HTML document using Jsoup
     - Extract the page title and content
     - Parse all links on the page
     - Add the page to the result set
     - Queue new discovered links (if depth limit allows)
   - Continue until the queue is empty or limits are reached

3. **Result**: Returns a `CrawlResult` containing all crawled pages, failed URLs, and statistics.

### Key Characteristics

- **Simple Architecture**: Linear flow from client → crawler → Jsoup → result
- **Breadth-First Traversal**: Processes all pages at depth N before moving to depth N+1
- **Predictable Execution**: Deterministic order of page visits
- **Low Complexity**: Easy to understand, debug, and maintain
- **Resource Efficient**: Minimal memory and thread overhead

### Diagram Reference

See [sequential-crawler-overview.png](./sequential-crawler-overview.png) for the detailed sequence diagram.

---

## Producer-Consumer Crawler (v2)

### Core Concept

The Producer-Consumer Crawler implements a **multi-threaded, parallel processing** pattern. It uses multiple worker threads that simultaneously crawl different pages, dramatically improving throughput for large-scale crawling.

### How It Works

1. **Initialization**:
   - The client creates a crawler specifying max depth, max pages, and **number of threads**
   - An `ExecutorService` thread pool is created with N worker threads
   - A shared `BlockingQueue` serves as the URL frontier

2. **Parallel Processing**:
   - The seed URL is added to the queue
   - Worker threads continuously poll the queue for URLs to process
   - Each worker:
     - Takes a URL from the queue (blocks if empty)
     - Fetches and parses the page with Jsoup
     - Extracts content and links
     - Adds the page to thread-safe result collections
     - **Produces** new URLs by adding discovered links back to the queue (if depth allows)
   - This continues until all URLs are processed and the queue is empty

3. **Thread Coordination**:
   - **ConcurrentHashMap** tracks visited URLs (thread-safe)
   - **AtomicInteger** counts pages without race conditions
   - **Synchronized lists** store results safely
   - Active worker tracking ensures no premature shutdown

4. **Clean Shutdown**:
   - Uses the **poison pill pattern**: a sentinel value that signals workers to stop
   - When the last worker finishes, it adds the poison pill to the queue
   - Other workers pass it along before terminating
   - The executor awaits termination of all threads

5. **Result**: Returns a `CrawlResult` with pages, failures, and performance statistics (duration, throughput).

### Key Characteristics

- **High Throughput**: Multiple pages processed simultaneously
- **Producer-Consumer Pattern**: Workers both consume URLs and produce new ones
- **Thread-Safe Design**: Uses concurrent collections and atomic operations
- **Scalable**: Performance scales with thread count (up to a point)
- **Complex Coordination**: Requires careful synchronization and shutdown logic
- **Non-Deterministic Order**: Pages may be crawled in unpredictable order

### Thread Safety Mechanisms

- **BlockingQueue**: Thread-safe URL frontier with built-in blocking operations
- **ConcurrentHashMap**: Lock-free visited URL tracking
- **AtomicInteger**: Lock-free page counting
- **Synchronized Collections**: Thread-safe result lists
- **Poison Pill Pattern**: Clean, coordinated shutdown without explicit locks

### Diagram Reference

See [producer-consumer-crawler-overview.png](./producer-consumer-crawler-overview.png) for the detailed sequence diagram showing multi-threaded interactions.

---

## Recursive Crawler (v3)

### Core Concept

The Recursive Crawler implements a **single-threaded, functional programming** pattern using the **trampoline technique**. It uses recursion to traverse the web graph while avoiding stack overflow through a sophisticated trampoline mechanism that converts recursive calls into iterative loops.

### How It Works

1. **Initialization**: The client creates a crawler using the builder pattern, specifying constraints like maximum depth and maximum pages.

2. **Trampoline Pattern**:
   - Instead of direct recursion, the crawler uses a `Trampoline` class that wraps recursive operations
   - Each recursive step returns either a `Done` result or a `More` continuation
   - The trampoline repeatedly executes continuations until a final result is reached
   - This prevents stack overflow even with deep recursion

3. **Recursive Processing**:
   - Starting with the seed URL, the crawler recursively processes each page
   - For each URL:
     - Fetch the HTML document using Jsoup
     - Extract the page title and content
     - Parse all links on the page
     - Add the page to the result set
     - Recursively process discovered links (if depth limit allows)
   - The trampoline ensures all recursive calls are stack-safe

4. **Result**: Returns a `CrawlResult` containing all crawled pages, failed URLs, and statistics.

### Key Characteristics

- **Functional Programming**: Elegant recursive design with immutable data structures
- **Stack Safety**: Trampoline pattern prevents stack overflow in deep recursion
- **Single-threaded**: Simple, predictable execution model
- **Breadth-First Traversal**: Processes pages level by level
- **Memory Efficient**: No explicit queue management needed
- **Elegant Code**: Clean, functional approach to web crawling

### Trampoline Pattern Benefits

- **Stack Safety**: Converts recursive calls into iterative loops
- **Deep Recursion**: Can handle arbitrarily deep web graphs
- **Functional Style**: Maintains the elegance of recursive programming
- **No Explicit Loops**: Recursive logic without manual iteration

### Diagram Reference

See [recursive-crawler-overview.png](./recursive-crawler-overview.png) for the detailed sequence diagram showing the trampoline pattern in action.

---

## Multi-threaded Recursive Crawler (v4)

### Core Concept

The Multi-threaded Recursive Crawler combines the **best of both worlds**: it merges the parallel processing power of the Producer-Consumer pattern with the elegant recursive design and stack safety of the trampoline pattern. This creates the most sophisticated and performant crawler implementation.

### How It Works

1. **Initialization**:
   - The client creates a crawler specifying max depth, max pages, and **number of threads**
   - An `ExecutorService` thread pool is created with N worker threads
   - A shared `BlockingQueue` serves as the URL frontier
   - Thread-safe collections manage shared state

2. **Hybrid Architecture**:
   - **Producer-Consumer Layer**: Multiple worker threads process URLs in parallel
   - **Recursive Layer**: Each worker uses trampoline pattern for stack-safe recursion
   - **Thread Coordination**: Synchronized access to shared resources

3. **Parallel Recursive Processing**:
   - The seed URL is added to the queue
   - Worker threads continuously poll the queue for URLs to process
   - Each worker:
     - Takes a URL from the queue (blocks if empty)
     - Uses trampoline pattern to recursively process the page
     - Fetches and parses the page with Jsoup
     - Extracts content and links
     - Adds the page to thread-safe result collections
     - **Produces** new URLs by adding discovered links back to the queue
   - This continues until all URLs are processed and the queue is empty

4. **Thread-Safe Trampoline**:
   - Each worker maintains its own trampoline instance
   - Shared state (visited URLs, results) is accessed through thread-safe collections
   - Recursive operations within each thread are stack-safe
   - No interference between parallel recursive operations

5. **Clean Shutdown**:
   - Uses the **poison pill pattern** for coordinated shutdown
   - Each worker completes its current recursive operation before terminating
   - The executor awaits termination of all threads

6. **Result**: Returns a `CrawlResult` with pages, failures, and performance statistics.

### Key Characteristics

- **Maximum Performance**: Parallel processing with stack-safe recursion
- **Hybrid Pattern**: Combines Producer-Consumer and Recursive patterns
- **Thread-Safe Recursion**: Multiple trampoline instances working in parallel
- **Scalable**: Performance scales with thread count
- **Stack Safety**: Deep recursion without stack overflow
- **Complex Coordination**: Sophisticated synchronization between threads

### Advanced Features

- **Parallel Trampolines**: Each thread has its own trampoline instance
- **Thread-Safe Collections**: ConcurrentHashMap, AtomicInteger, synchronized lists
- **Coordinated Shutdown**: Poison pill pattern with recursive operation completion
- **Optimal Resource Usage**: Balances parallelism with memory efficiency

### Diagram Reference

See [multi-threaded-recursive-crawler-overview.png](./multi-threaded-recursive-crawler-overview.png) for the detailed sequence diagram showing the hybrid architecture with parallel recursive processing.

---

## Comparison

| Aspect | Sequential Crawler (v1) | Producer-Consumer Crawler (v2) | Recursive Crawler (v3) | Multi-threaded Recursive Crawler (v4) |
|--------|------------------------|--------------------------------|------------------------|--------------------------------------|
| **Threading** | Single-threaded | Multi-threaded (configurable) | Single-threaded | Multi-threaded (configurable) |
| **Throughput** | Low (one page at a time) | High (N pages simultaneously) | Low (one page at a time) | High (N pages simultaneously) |
| **Complexity** | Simple | Complex | Medium | Very Complex |
| **Resource Usage** | Low | Higher (threads, memory) | Low | Higher (threads, memory) |
| **Order** | Deterministic breadth-first | Non-deterministic | Deterministic breadth-first | Non-deterministic |
| **Scalability** | Limited | Scales with cores/threads | Limited | Scales with cores/threads |
| **Debugging** | Easy | More challenging | Medium | Most challenging |
| **Stack Safety** | N/A (iterative) | N/A (iterative) | Yes (trampoline) | Yes (trampoline) |
| **Programming Style** | Imperative | Imperative | Functional | Hybrid |
| **Use Case** | Small sites, prototyping | Large sites, production | Deep sites, functional programming | Large deep sites, maximum performance |

## Architecture Insights

### Sequential Crawler Pattern

The sequential approach follows the classic **Queue-based BFS (Breadth-First Search)** algorithm:

```
Queue ← [seed URL]
Visited ← {}

while Queue is not empty:
    url ← dequeue()
    if url in Visited: continue

    page ← fetch(url)
    process(page)

    Visited.add(url)
    for link in page.links:
        if link not in Visited:
            enqueue(link)
```

### Producer-Consumer Pattern

The multi-threaded approach implements the classic **Producer-Consumer Pattern**:

```
Shared Queue ← [seed URL]
Visited ← ConcurrentHashMap

Workers (parallel):
    while true:
        url ← queue.poll(timeout)
        if url is POISON_PILL: break
        if url is null: continue

        if Visited.putIfAbsent(url): continue

        page ← fetch(url)
        results.add(page)  // synchronized

        for link in page.links:
            queue.offer(link)  // producer role
```

Key insight: Each worker is both a **consumer** (taking URLs from the queue) and a **producer** (adding discovered URLs to the queue), creating a dynamic work-stealing pattern.

### Recursive Pattern with Trampoline

The recursive approach implements the **Trampoline Pattern** for stack-safe recursion:

```
function crawlRecursive(url, depth):
    if depth > maxDepth: return Done(result)

    page ← fetch(url)
    result.add(page)

    tasks ← []
    for link in page.links:
        tasks.add(() -> crawlRecursive(link, depth + 1))

    return More(() -> trampoline(tasks))

// Trampoline execution
while (step = trampoline.step()) is More:
    trampoline = step.continuation()
return step.result()
```

Key insight: The trampoline converts recursive calls into iterative loops, preventing stack overflow while maintaining the elegance of functional programming.

### Multi-threaded Recursive Pattern

The hybrid approach combines **Producer-Consumer** and **Trampoline** patterns:

```
Shared Queue ← [seed URL]
Visited ← ConcurrentHashMap
Thread Pool ← N workers

Workers (parallel):
    while true:
        url ← queue.poll(timeout)
        if url is POISON_PILL: break

        // Each worker has its own trampoline
        trampoline ← new Trampoline()
        result ← trampoline.execute(() -> crawlRecursive(url, 0))

        for link in result.newLinks:
            queue.offer(link)  // producer role
```

Key insight: This hybrid pattern achieves maximum performance by combining parallel processing with stack-safe recursion, where each thread maintains its own trampoline instance.

## Diagrams

All four implementations are documented with PlantUML sequence diagrams:

1. **Sequential Crawler (v1)**: [sequential-crawler-overview.png](./sequential-crawler-overview.png)
   - Shows simple, linear flow
   - Single crawler instance handling all operations
   - ![Sequential Crawler Overview](./sequential-crawler-overview.png)

2. **Producer-Consumer Crawler (v2)**: [producer-consumer-crawler-overview.png](./producer-consumer-crawler-overview.png)
   - Shows parallel worker threads
   - BlockingQueue coordination
   - Poison pill shutdown pattern
   - ![Producer-Consumer Crawler Overview](./producer-consumer-crawler-overview.png)

3. **Recursive Crawler (v3)**: [recursive-crawler-overview.png](./recursive-crawler-overview.png)
   - Shows trampoline pattern in action
   - Stack-safe recursive processing
   - Functional programming approach
   - ![Recursive Crawler Overview](./recursive-crawler-overview.png)

4. **Multi-threaded Recursive Crawler (v4)**: [multi-threaded-recursive-crawler-overview.png](./multi-threaded-recursive-crawler-overview.png)
   - Shows hybrid architecture with parallel recursive processing
   - Thread-safe trampoline coordination
   - Maximum performance design
   - ![Multi-threaded Recursive Crawler Overview](./multi-threaded-recursive-crawler-overview.png)

## Conclusion

All four crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements:

- **Choose Sequential (v1)** for simplicity, predictability, and smaller crawling tasks
- **Choose Producer-Consumer (v2)** for performance, scalability, and large-scale crawling operations
- **Choose Recursive (v3)** for elegant functional programming and deep recursion without stack overflow
- **Choose Multi-threaded Recursive (v4)** for maximum performance with stack-safe deep recursion

The project demonstrates how the same problem can be solved with different architectural patterns and concurrency models:

- **v1**: Simple iterative approach with queue-based BFS
- **v2**: Multi-threaded producer-consumer pattern for parallel processing
- **v3**: Functional programming with trampoline pattern for stack-safe recursion
- **v4**: Hybrid approach combining parallel processing with stack-safe recursion

Each implementation offers unique tradeoffs between simplicity, performance, and programming paradigm, showcasing different approaches to solving complex concurrent programming challenges.
