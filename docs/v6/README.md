# Recursive Actor Crawler (v6)

## Core Concept

The Recursive Actor Crawler implements a **hybrid actor model with recursive design** that combines the best of both the Actor Model pattern and recursive programming. It uses dynamic actor spawning where each actor can recursively create child actors for discovered links, creating a natural tree-like crawling structure that matches the web topology.

## How It Works

1. **Initialization**:
   - The client creates a crawler specifying max depth, max pages, and **maximum number of actors**
   - A root `RecursiveActor` is created to coordinate the crawling process
   - Thread-safe collections manage shared state across all actors
   - An `ExecutorService` provides the execution context for asynchronous operations

2. **Recursive Actor Architecture**:
   - **Root Actor**: Starts the crawling process from the seed URL
   - **Child Actors**: Dynamically spawned for each discovered link
   - **Shared State**: All actors share thread-safe collections (visited URLs, results, counters)
   - **Asynchronous Processing**: Each actor uses `CompletableFuture` for non-blocking execution

3. **Recursive Processing Flow**:
   - The root actor processes the seed URL asynchronously
   - For each discovered link, a new child actor is spawned
   - Each child actor:
     - Processes its assigned URL asynchronously
     - Fetches and parses the page with Jsoup
     - Extracts content and links
     - Adds the page to shared result collections
     - Spawns its own child actors for discovered links (if depth allows)
   - This creates a natural tree structure matching the web topology

4. **Dynamic Actor Management**:
   - Actors are created on-demand based on discovered links
   - Each actor maintains a list of its child actors
   - Shared state coordination through thread-safe collections
   - Automatic cleanup when actors complete their work

5. **Fault Isolation and Coordination**:
   - Each actor operates independently and can fail without affecting others
   - Child actor failures are handled gracefully and logged
   - Shared state ensures consistent results across all actors
   - Asynchronous processing prevents blocking on individual failures

6. **Result**: Returns a `CrawlResult` with pages, failures, and performance statistics.

## Key Characteristics

- **Hybrid Architecture**: Combines Actor Model with recursive design
- **Dynamic Actor Spawning**: Actors created on-demand for discovered links
- **Natural Tree Structure**: Crawling structure matches web topology
- **Asynchronous Processing**: CompletableFuture-based actors for non-blocking operations
- **Fault Isolation**: Actor failures don't affect other branches
- **Shared State Coordination**: Thread-safe collections for consistent results
- **Stack-Safe Recursion**: Asynchronous execution prevents stack overflow
- **Resource Management**: Dynamic actor creation and cleanup

## Recursive Actor Benefits

- **Natural Structure**: Tree-like crawling matches web topology
- **Dynamic Scaling**: Actors created based on actual link discovery
- **Fault Tolerance**: Independent actor operation with graceful failure handling
- **Asynchronous Safety**: CompletableFuture prevents blocking and stack overflow
- **Shared Coordination**: Thread-safe state management across actors
- **Resource Efficiency**: Actors created only when needed

## Architecture Pattern

The recursive actor approach implements **Dynamic Actor Spawning** with shared state coordination:

```
RootActor ← [seed URL]
SharedState ← {visitedUrls, results, counters}
ChildActors ← []

function processUrl(url, depth):
    if depth > maxDepth: return

    page ← fetch(url)
    sharedState.results.add(page)

    for link in page.links:
        if not sharedState.visitedUrls.contains(link):
            childActor ← new RecursiveActor(sharedState)
            childActors.add(childActor)
            childActor.processUrl(link, depth + 1)  // async

    CompletableFuture.allOf(childActors).join()
```

Key insight: This pattern creates a natural tree structure where actors spawn child actors for discovered links, matching the web topology while maintaining shared state coordination.

## Diagram Reference

See [recursive-actor-crawler-overview.png](./recursive-actor-crawler-overview.png) for the detailed sequence diagram showing the recursive actor model with dynamic spawning.

## Use Case

Choose Recursive Actor (v6) for tree-like sites with dynamic actor spawning and natural web topology matching.
