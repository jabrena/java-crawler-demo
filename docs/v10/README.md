# Virtual Thread Actor Crawler (v10)

## Core Concept

The Virtual Thread Actor Crawler implements the **Actor Model pattern** using **virtual threads** and **asynchronous message passing**. It's a modernized version of V5 that replaces CompletableFuture with virtual threads, providing simplified async processing, better resource utilization, and easier debugging while maintaining the same supervisor-worker architecture and fault tolerance characteristics.

## How It Works

1. **Initialization**:
   - The client creates a crawler specifying max depth, max pages, and **number of actors**
   - A `SupervisorActor` is created to coordinate the crawling process
   - Multiple `WorkerActor` instances are created for parallel URL processing
   - Virtual thread executors manage lightweight concurrency
   - Thread-safe collections manage shared state and message queues

2. **Actor System Architecture**:
   - **Supervisor Actor**: Coordinates the overall crawling process, manages worker actors, and aggregates results
   - **Worker Actors**: Process individual URLs asynchronously using virtual threads
   - **Message Passing**: Actors communicate through immutable message objects (CrawlMessage, ResultMessage, ErrorMessage, CompletionMessage)
   - **Virtual Thread Processing**: All operations use virtual threads for lightweight concurrency

3. **Message-Driven Processing**:
   - The seed URL is wrapped in a `CrawlMessage` and sent to the supervisor
   - The supervisor distributes `CrawlMessage` objects to available worker actors
   - Each worker actor:
     - Receives a `CrawlMessage` with URL and depth information
     - Fetches and parses the page with Jsoup using virtual threads
     - Extracts content and links
     - Sends back a `ResultMessage` with the page data and discovered links
     - Sends a `CompletionMessage` to signal task completion
   - The supervisor processes messages and queues new URLs for crawling

4. **Coordination Loop**:
   - The supervisor runs a coordination loop that:
     - Processes pending messages from worker actors
     - Distributes new work to available workers
     - Tracks visited URLs to avoid duplicates
     - Aggregates results from all workers
     - Detects when crawling is complete
   - Uses thread-safe collections for coordination without explicit locks

5. **Fault Tolerance**:
   - Each actor operates independently and can fail without affecting others
   - Error messages are handled gracefully and logged
   - The supervisor pattern provides fault isolation
   - Virtual thread processing prevents blocking on individual failures

6. **Result**: Returns a `CrawlResult` with pages, failures, and performance statistics.

## Key Characteristics

- **Actor Model**: Message-passing concurrency with no shared mutable state
- **Virtual Threads**: Lightweight concurrency without thread pool overhead
- **Simplified Async Processing**: Natural blocking code instead of complex CompletableFuture chains
- **Fault Tolerance**: Supervisor pattern provides fault isolation and recovery
- **Loose Coupling**: Actors communicate only through immutable messages
- **High Scalability**: Can handle large numbers of concurrent operations
- **Thread-Safe Design**: Uses concurrent collections and atomic operations
- **Message-Driven**: All coordination happens through message passing

## Virtual Thread Benefits

- **Lightweight Concurrency**: Virtual threads use minimal memory and are cheap to create/destroy
- **Simplified Code**: No complex async chaining - just natural blocking code patterns
- **Better Resource Utilization**: Efficient concurrency without thread pool overhead
- **Easier Debugging**: Clean stack traces and simpler debugging compared to async chains
- **Natural Blocking**: Can use blocking I/O operations without performance penalties
- **No Thread Pool Management**: Virtual threads are managed by the JVM automatically

## Key Differences from V5 (CompletableFuture-based)

- **Virtual Threads vs CompletableFuture**: Uses virtual threads for async processing instead of CompletableFuture
- **Simpler Code**: No complex async chaining and callback management
- **Better Resource Utilization**: Virtual threads are more efficient than traditional thread pools
- **Easier Debugging**: Clean stack traces without async chain complexity
- **Natural Blocking**: Can use blocking operations without performance concerns
- **No Thread Pool Complexity**: Virtual threads eliminate thread pool management overhead

## Message Types

- **CrawlMessage**: Request to crawl a specific URL at a given depth
- **ResultMessage**: Successful crawl result with page data and discovered links
- **ErrorMessage**: Error that occurred while crawling a URL
- **CompletionMessage**: Signal that a worker has completed its current task

## Architecture Pattern

The virtual thread actor-based approach implements the **Actor Model** with message passing:

```
SupervisorActor ← [seed URL]
MessageQueue ← []
WorkerActors ← [N actors with virtual threads]

Supervisor Loop:
    while not complete:
        processMessages()  // Handle ResultMessage, ErrorMessage, CompletionMessage
        distributeWork()   // Send CrawlMessage to available workers
        checkCompletion()  // Check if crawling is complete

Worker Actors (parallel with virtual threads):
    while active:
        message ← receive(CrawlMessage)
        page ← fetch(message.url)  // async with virtual threads
        links ← extractLinks(page)
        send(ResultMessage(page, links, depth))
        send(CompletionMessage())
```

Key insight: Virtual threads provide the benefits of the actor model with simplified async processing, eliminating the complexity of CompletableFuture chains while maintaining fault tolerance and scalability.

## Diagram Reference

![Virtual Thread Actor Crawler Overview](./virtual-thread-actor-crawler-overview.png)

## Use Case

Choose Virtual Thread Actor (v10) for modern Java applications that want the benefits of the actor model with simplified async processing and better resource utilization.
