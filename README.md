# Spool — Crawler

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/build-Maven-C71A36.svg)](https://maven.apache.org/)

**Crawler** is the **Spool** framework module responsible for **fetching raw data from external sources and delivering it to the inbox**. It handles the full polling pipeline — fetch → normalize → split → persist — while publishing lifecycle events and routing errors uniformly.

---

## Table of Contents

- [What it does](#what-it-does)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Examples](#examples)
  - [Custom PollSource](#custom-pollsource)
  - [Scheduling](#scheduling)
  - [Domain Event Mapping](#domain-event-mapping)
  - [Watchdog Integration](#watchdog-integration)
  - [Custom Error Routing](#custom-error-routing)
- [Key Concepts](#key-concepts)
- [Built-in Normalizer Formats](#built-in-normalizer-formats)
- [License](#license)

---

## What it does

```
PollSource.fetch()
     │
     ▼
Normalizer (deserialize → locate → split → enrich → serialize)
     │
     ▼
InboxWriter.receive(Envelope)
     │
     ▼
EventPublisher.publish(SourceItemCaptured / InboxItemStored)

Errors ──► ErrorRouter ──► failure event
```

You implement **one interface** (`PollSource`) to fetch data from anywhere — HTTP, database, file, queue. The Crawler handles normalization, inbox persistence, and event emission.

All modules are assembled and started through `SpoolNode`.

---

## Installation

```xml
<dependency>
    <groupId>io.github.spool-framework</groupId>
    <artifactId>crawler</artifactId>
    <version>1.0.2-SNAPSHOT</version>
</dependency>
```

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/Spool-FRAMEWORK/*</url>
    </repository>
</repositories>
```

> Requires **Java 21+**.

---

## Quick Start

```java
// Use the built-in HTTP source from spool-infrastructure
PollSource<byte[]> source = new HTTPPollSource("https://api.example.com/records", "my-api");

// Build the crawler
Crawler crawler = CrawlerBuilderFactory.poll(source)
        .source()
            .ports(CrawlerPorts.builder()
                    .inbox(myInboxWriter)
                    .bus(eventBus)
                    .build())
            .and()
        .createWith(StandardNormalizer.JSON_ARRAY);

// Register with a SpoolNode and start
SpoolNode node = SpoolNode.create()
        .register(crawler);

node.start();
```

---

## Examples

### Custom PollSource

Implement `PollSource<R>` when you need a custom fetch strategy — database query, file read, or any other source.

```java
public class OrderSource implements PollSource<ResultSet> {
    private Connection connection;

    @Override
    public PollSource<ResultSet> open() {
        connection = DriverManager.getConnection("jdbc:postgresql://localhost/mydb", "user", "pass");
        return this;
    }

    @Override
    public ResultSet fetch() throws SpoolException {
        try {
            return connection.createStatement()
                    .executeQuery("SELECT id, name, total FROM orders WHERE status = 'NEW'");
        } catch (SQLException e) {
            throw new SpoolException("Failed to query orders", e);
        }
    }

    @Override
    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }

    @Override
    public String sourceId() { return "orders-db"; }
}

Crawler crawler = CrawlerBuilderFactory.poll(new OrderSource())
        .source()
            .ports(CrawlerPorts.builder()
                    .inbox(myInboxWriter)
                    .bus(eventBus)
                    .build())
            .and()
        .createWith(StandardNormalizer.RESULT_SET);
```

---

### Scheduling

Control how often the crawler polls the source.

```java
Crawler crawler = CrawlerBuilderFactory.poll(source)
        .source()
            .ports(ports)
            .schedule(PollingConfiguration.every(Duration.ofMinutes(5)))
            .and()
        .createWith(StandardNormalizer.JSON_ARRAY);
```

---

### Domain Event Mapping

Automatically deserialize each captured record into a domain event and emit it on the bus.

```java
// Map each record to a domain event via a DTO
Crawler crawler = CrawlerBuilderFactory.poll(source)
        .source()
            .ports(ports)
            .and()
        .mapping()
            .convention(NamingConvention.SNAKE_CASE)
            .addDomainEvent(OrderDTO.class, (dto, key) ->
                    new OrderReceived(dto.id(), dto.total(), key), "orderId")
            .and()
        .createWith(StandardNormalizer.JSON_ARRAY);
```

---

### Watchdog Integration

Register the crawler with the Watchdog so it sends periodic heartbeats.

```java
Crawler crawler = CrawlerBuilderFactory
        .watchdog("http://spool-watchdog:8080", "my-crawler")
        .poll(source)
        .source()
            .ports(ports)
            .and()
        .createWith(StandardNormalizer.JSON_ARRAY);
```

---

### Custom Error Routing

Add logging, metrics, or recovery logic for specific failure types.

```java
ErrorRouter errorRouter = new ErrorRouter()
        .on(SourcePollException.class, (e, ctx) ->
                logger.error("Fetch failed for source {}: {}", source.sourceId(), e.getMessage()))
        .on(InboxWriteException.class, (e, ctx) ->
                metrics.increment("inbox.write.failures"));

Crawler crawler = CrawlerBuilderFactory.poll(source)
        .source()
            .ports(ports)
            .and()
        .observability()
            .withErrorRouter(errorRouter)
            .and()
        .createWith(StandardNormalizer.JSON_ARRAY);
```

---

## Key Concepts

| Concept | Description |
|---|---|
| **`PollSource<R>`** | Interface you implement to fetch raw data. Provides `fetch()`, `open()`, `close()`, `sourceId()`. |
| **`StreamSource<R>`** | Interface for push-based sources (Kafka, WebSocket…). |
| **`StandardNormalizer`** | Built-in normalizer formats: `JSON_ARRAY`, `JSON_OBJECT`, `YAML_ARRAY`, `RESULT_SET`, `IN_MEMORY`. |
| **`NormalizerFormat<P>`** | Functional interface for custom normalization pipelines. |
| **`CrawlerPorts`** | Bundles `InboxWriter` and `EventPublisher`. |
| **`CrawlerBuilderFactory`** | DSL entry point. Returns a `PollingCrawlerBuilder` or `StreamCrawlerBuilder`. |
| **`SpoolNode`** | Assembles and starts modules. `SpoolNode.create().register(crawler).start()`. |

---

## Built-in Normalizer Formats

| Constant | Input | Description |
|---|---|---|
| `StandardNormalizer.JSON_ARRAY` | `byte[]` | Parse JSON array → split → serialize each element. |
| `StandardNormalizer.JSON_OBJECT` | `byte[]` | Parse a single JSON object → serialize. |
| `StandardNormalizer.YAML_ARRAY` | `byte[]` | Parse YAML sequence → split → serialize each element. |
| `StandardNormalizer.RESULT_SET` | `ResultSet` | Project each row → serialize to JSON. |
| `StandardNormalizer.IN_MEMORY` | `Object` | Pass objects directly without serialization. |

---

## License

Distributed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
