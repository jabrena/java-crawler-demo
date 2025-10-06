# Web Crawler Architecture Overview

This document explains the two different web crawler implementations in this project: the Sequential Crawler and the Producer-Consumer Crawler. Both approaches solve the same problem—crawling web pages starting from a seed URL—but use fundamentally different architectural patterns.

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

See [sequential-crawler-overview.puml](./sequential-crawler-overview.puml) for the detailed sequence diagram.

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

See [producer-consumer-crawler-overview.puml](./producer-consumer-crawler-overview.puml) for the detailed sequence diagram showing multi-threaded interactions.

---

## Comparison

| Aspect | Sequential Crawler | Producer-Consumer Crawler |
|--------|-------------------|---------------------------|
| **Threading** | Single-threaded | Multi-threaded (configurable) |
| **Throughput** | Low (one page at a time) | High (N pages simultaneously) |
| **Complexity** | Simple | Complex |
| **Resource Usage** | Low | Higher (threads, memory) |
| **Order** | Deterministic breadth-first | Non-deterministic |
| **Scalability** | Limited | Scales with cores/threads |
| **Debugging** | Easy | More challenging |
| **Use Case** | Small sites, prototyping | Large sites, production |

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

## Diagrams

Both implementations are documented with PlantUML sequence diagrams:

1. **Sequential Crawler**: [sequential-crawler-overview.puml](./sequential-crawler-overview.puml)
   - Shows simple, linear flow
   - Single crawler instance handling all operations

2. **Producer-Consumer Crawler**: [producer-consumer-crawler-overview.puml](./producer-consumer-crawler-overview.puml)
   - Shows parallel worker threads
   - BlockingQueue coordination
   - Poison pill shutdown pattern

To view these diagrams:
- Use a PlantUML viewer/plugin in your IDE
- Use online PlantUML renderers
- Generate PNG images with PlantUML CLI: `plantuml *.puml`

## Conclusion

Both crawlers share the same public API and produce the same `CrawlResult` structure, making them interchangeable from a client perspective. The choice between them depends on your specific requirements:

- **Choose Sequential** for simplicity, predictability, and smaller crawling tasks
- **Choose Producer-Consumer** for performance, scalability, and large-scale crawling operations

The project demonstrates how the same problem can be solved with different concurrency models, each with its own tradeoffs between simplicity and performance.
