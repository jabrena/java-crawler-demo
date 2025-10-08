# Multi-threaded Recursive Crawler (v4)

## Core Concept

The Multi-threaded Recursive Crawler combines the **best of both worlds**: it merges the parallel processing power of the Producer-Consumer pattern with the elegant recursive design and stack safety of the trampoline pattern. This creates the most sophisticated and performant crawler implementation.

## How It Works

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

## Key Characteristics

- **Maximum Performance**: Parallel processing with stack-safe recursion
- **Hybrid Pattern**: Combines Producer-Consumer and Recursive patterns
- **Thread-Safe Recursion**: Multiple trampoline instances working in parallel
- **Scalable**: Performance scales with thread count
- **Stack Safety**: Deep recursion without stack overflow
- **Complex Coordination**: Sophisticated synchronization between threads

## Advanced Features

- **Parallel Trampolines**: Each thread has its own trampoline instance
- **Thread-Safe Collections**: ConcurrentHashMap, AtomicInteger, synchronized lists
- **Coordinated Shutdown**: Poison pill pattern with recursive operation completion
- **Optimal Resource Usage**: Balances parallelism with memory efficiency

## Architecture Pattern

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

## Diagram Reference

![Multi-threaded Recursive Crawler Overview](./multi-threaded-recursive-crawler-overview.png)

## Use Case

Choose Multi-threaded Recursive (v4) for large deep sites and maximum performance.
