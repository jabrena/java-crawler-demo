# Java Crawler Demo

A web crawler implementations in Java.

## Design alternatives

- [V1: Sequential](./docs/sequential-crawler-overview.png)
- [V2: Producer-Consumer Pattern (Multi-threaded)](./docs/producer-consumer-crawler-overview.png)
- Recursive Design
- Event-Driven/Reactive Design
- Actor Model (Message-Passing)
- Pipeline/Chain of Responsibility
- Strategy Pattern (Pluggable Components)
- Visitor Pattern (Content Processing)

## How to test

```bash
./mvnw compile exec:java -Pexamples -Dexec.mainClass="info.jab.crawler.v1.SequentialCrawlerExample"
./mvnw compile exec:java -Pexamples -Dexec.mainClass="info.jab.crawler.v2.ProducerConsumerCrawlerExample"
```

## Key Design Considerations Across All Approaches:

- URL Frontier Management: Queue, Priority Queue, or Database-backed
- Politeness: Rate limiting, respecting robots.txt
- Deduplication: Visited URL tracking (Set, Bloom Filter, Database)
- Error Handling: Retry logic, circuit breakers
- State Management: In-memory, persistent, or distributed
- Parsing Strategy: CSS selectors vs XPath vs DOM traversal
- Data Extraction: Direct extraction vs Visitor pattern
- Storage: In-memory, files, databases

Powered by [Cursor](https://www.cursor.com/) with ❤️ from [Madrid](https://www.google.com/maps/place/Community+of+Madrid,+Madrid/@40.4983324,-6.3162283,8z/data=!3m1!4b1!4m6!3m5!1s0xd41817a40e033b9:0x10340f3be4bc880!8m2!3d40.4167088!4d-3.5812692!16zL20vMGo0eGc?entry=ttu&g_ep=EgoyMDI1MDgxOC4wIKXMDSoASAFQAw%3D%3D)
