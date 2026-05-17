# Log Ingestor — Architecture

## Overview

A full-stack log ingestion and search system. Users can ingest JSON log entries via a REST API and search/filter them through a web UI. The system uses a **React + TypeScript frontend** that talks to a **Play Framework (Scala) backend**. Incoming logs are buffered through **Apache Kafka** for durability and back-pressure, then consumed asynchronously and written to **PostgreSQL** and optionally **Elasticsearch**.

```
┌──────────────────────┐     HTTP (REST)      ┌────────────────────────────┐
│   Frontend (React)   │ ◄──────────────────► │  Backend (Play/Scala)     │
│   Port 3001          │   POST /api/filter   │  Port 3000                │
│   (nginx reverse     │   POST /             │                           │
│    proxy)            │                      │                           │
└──────────────────────┘                      └───────────┬────────────────┘
                                                          │
                                                          │ Kafka Producer
                                                          ▼
                                                  ┌────────────────┐
                                                  │   Kafka        │
                                                  │   logs-raw     │
                                                  │   topic        │
                                                  └────────┬───────┘
                                                           │
                                                           │ Consumer Group
                                                           ▼
                                                  ┌────────────────────┐
                                                  │ Consumer (Scala)   │
                                                  │ - Parse           │
                                                  │ - Batch write     │
                                                  │ - DLQ on failure  │
                                                  └──┬─────────────┬──┘
                                                     │             │
                                                     ▼             ▼
                                              ┌──────────┐  ┌──────────────┐
                                              │PostgreSQL│  │Elasticsearch │
                                              │(primary) │  │(optional)    │
                                              └──────────┘  └──────────────┘
```

---

## Frontend (`log_ingestor_frontend/`)

### Stack
| Layer        | Technology                              |
|-------------|-----------------------------------------|
| Framework   | React 18.2                              |
| Language    | TypeScript 4.9                          |
| Build Tool  | Create React App (react-scripts 5.0.1)  |
| HTTP Client | Axios 1.6.2                             |
| Date Picker | react-datepicker 4.21.0 + date-fns 2.30 |
| Styling     | Plain CSS (no framework)                |

### Structure

```
src/
├── index.tsx                 Entry point – renders <App /> into #root
├── index.css                 Global base styles (body, code, resets)
├── App.tsx                   Root component – tab navigation (Search / Ingest)
├── App.css                   All component & layout styles
├── types.ts                  Shared TypeScript interfaces (Log)
├── SearchLogsComponent.tsx   Log search & results
├── IngestLogsComponent.tsx   Log ingestion form
└── react-app-env.d.ts        CRA type declarations
```

### Pages / Views

1. **Search Logs** — A query-type selector (level / message / resource ID / timestamp range) followed by conditional filter inputs and a search button. Results are displayed as cards.

2. **Ingest Logs** — A form with fields for each log property (level, message, resourceId, traceId, spanId, commit, parentResourceId) that POSTs to the backend.

### Data Flow

```
User Input → Component State (searchParam) → Axios POST → API Response → Component State (apiResponse) → Re-render
```

### API Calls

| Method | Endpoint              | Purpose            | Called From             |
|--------|-----------------------|--------------------|-------------------------|
| POST   | `/api/filter`         | Search logs        | SearchLogsComponent     |
| POST   | `/`                   | Ingest log(s)      | IngestLogsComponent     |

---

## Backend (`log_ingestor_backend/`)

### Stack
| Layer        | Technology                 |
|-------------|----------------------------|
| Framework   | Play Framework 3.0.0       |
| Language    | Scala 2.13                 |
| Build Tool  | sbt 1.7.1 (primary) / Mill |
| Database    | PostgreSQL                 |
| DB Access   | ScalikeJDBC 3.5.0          |
| JSON        | play-json 2.10.0           |
| DI          | Google Guice               |
| Migrations  | Play Evolutions            |

### Layered Architecture

```
HTTP Request
    │
    ▼
  Routes (conf/routes)
    │
    ▼
  Controller (LogController.scala)
    │  JSON validation, parameter extraction, response formatting
    ▼
  Service (LogService.scala)
    │  Business logic, batch splitting, validation
    ▼
  DAO (LogDao.scala)
    │  SQL queries via ScalikeJDBC
    ▼
  PostgreSQL
```

### Routes

| Method | Path         | Controller Action         | Description           |
|--------|--------------|---------------------------|-----------------------|
| GET    | `/`          | HomeController.index()    | Welcome page (Twirl)  |
| POST   | `/`          | LogController.ingestLog() | Ingest log entries    |
| POST   | `/api/filter`| LogController.searchLogs()| Search / filter logs  |
| GET    | `/assets/*`  | Assets.versioned()        | Static files          |

### Controllers

**`LogController.scala`**
- `ingestLog()` — Accepts `List[LogInput]` JSON. Validates, calls `LogService.insertLogs()`, returns success/error response.
- `searchLogs()` — Accepts `SearchParam` JSON. Dispatches to the correct service method based on `QueryType`:
  - `LevelFilter` → `getLogsByLevel(level)`
  - `MessageFilter` → `getLogsByMessage(search_string)`
  - `ResourceIdFilter` → `getLogsByResource(resource_id)`
  - `TimestampFilter` → `getLogsByRange(fromTime, tillTime)`

### Service Layer

**`LogService.scala`**
- `insertLogs()` — Splits log list into batches of 100 and inserts each batch in a transaction.
- `getLogsByLevel(level)` — Delegates to DAO.
- `getLogsByMessage(searchString)` — Uses SQL `LIKE '%searchString%'`.
- `getLogsByResource(resourceId)` — Delegates to DAO.
- `getLogsByRange(fromTime, tillTime)` — Uses SQL `BETWEEN`.

### Data Access (DAO)

**`LogDao.scala`**
- Uses `DB.localTx` for transactional batch inserts.
- All search queries use `LIKE` for pattern matching.
- Each search method returns `List[LogInput]` converted from result sets.

### Models

**`LogModels.scala`**
```scala
case class LogInput(
  level: String,
  message: String,
  resource_id: String,
  timestamp: DateTime,
  trace_id: String,
  span_id: String,
  commit: String,
  metadata: Option[LogInputMetadata]
)
case class LogInputMetadata(parent_resource_id: String)
```

**`SearchParam.scala`**
```scala
case class SearchParam(
  query: Option[QueryType],
  level: Option[String],
  search_string: Option[String],
  resource_id: Option[String],
  fromTime: Option[DateTime],
  tillTime: Option[DateTime]
)
```

`QueryType` is a sealed trait: `LevelFilter`, `MessageFilter`, `ResourceIdFilter`, `TimestampFilter`.

### Database (`logs` table)

```sql
CREATE TABLE logs (
  level             TEXT,
  message           TEXT,
  resource_id       TEXT,
  timestamp         TIMESTAMPTZ,
  trace_id          TEXT,
  span_id           TEXT,
  commit            TEXT,
  parent_resource_id TEXT
);
```

**Indexes** (Evolution 2): Individual indexes on `level`, `message`, `resource_id`, `timestamp`, `trace_id`, `span_id`, `commit`, plus a composite index on `(level, timestamp)`.

### Message Queue Publisher

**`MessageQueuePublisher` trait** — abstraction over message queue publication:
- `publish(logs: Seq[LogInput]): Future[Unit]`
- `close(): Unit`

**`KafkaMessageQueuePublisher`** — Kafka implementation using async send with callback (no blocking `.get()`):
- Configurable bootstrap servers, topic, acks, retries, linger.ms, request.timeout.ms
- Graceful shutdown via Play's `ApplicationLifecycle` hook
- SLF4J logging

**`LogIngestionService`** — orchestrates ingestion through the publisher trait.

### Dependency Injection

- **`AppApplicationLoader`** — Custom `GuiceApplicationLoader` that calls `DBs.setupAll()` on startup.
- **`LogModule`** — Guice module that binds `MessageQueuePublisher → KafkaMessageQueuePublisher`, `LogIngestionService`, `LogService`, `LogDao`, and `LogController` as eager singletons.

### Configuration (`application.conf`)

| Key                    | Value                                     |
|------------------------|-------------------------------------------|
| Database URL           | `jdbc:postgresql://localhost:5432/logs_ingestor_db` |
| Server Port            | 3000                                      |
| CORS Origins           | `http://localhost:3001`                    |
| CORS Methods           | GET, POST, PUT, DELETE, OPTIONS           |
| CORS Headers           | Content-Type, Authorization               |
| Kafka Bootstrap        | `localhost:9092`                          |
| Kafka Topic            | `logs-raw`                                |
| Producer Acks          | `1`                                       |
| Producer Retries       | `3`                                       |

---

## API Specification

### `POST /api/filter` — Search Logs

**Request body:**
```json
// Level search
{ "query": "level", "level": "error" }

// Message substring search
{ "query": "message", "search_string": "timeout" }

// Resource ID search
{ "query": "resource", "resource_id": "abc-123" }

// Timestamp range search
{ "query": "timestamp", "fromTime": "2024-01-01T00:00:00.000Z", "tillTime": "2024-01-31T23:59:59.000Z" }
```

**Response:** `Array` of log objects:
```json
[
  {
    "level": "error",
    "message": "Connection timeout",
    "resourceId": "abc-123",
    "timestamp": "2024-01-15T10:30:00.000Z",
    "traceId": "trace-001",
    "spanId": "span-001",
    "commit": "abc123",
    "metadata": { "parentResourceId": "xyz-789" }
  }
]
```

### `POST /` — Ingest Logs

**Request body:** `Array` of log objects (same shape as above).

**Response:** `{ "status": "ok", "message": "Logs ingested successfully" }` or error.

---

## Development

### Prerequisites
- Node.js 16+
- Java 11+
- sbt 1.7+
- PostgreSQL running on `localhost:5432` with database `logs_ingestor_db`

### Running

```bash
# Backend
cd log_ingestor_backend
sbt run        # Starts on http://localhost:3000

# Frontend
cd log_ingestor_frontend
npm install
npm start      # Starts on http://localhost:3001
```

---

## Consumer (`log_ingestor_consumer/`)

### Stack
| Layer             | Technology                           |
|------------------|--------------------------------------|
| Language         | Scala 2.13                           |
| Build Tool       | sbt (JavaAppPackaging)               |
| Kafka Client     | kafka-clients 3.5.0                  |
| Database Access  | ScalikeJDBC 3.5.0 (PostgreSQL)       |
| Search Engine    | elasticsearch-rest-client 8.10.2     |
| JSON             | play-json 2.10.0                     |
| Logging          | SLF4J + Logback 1.4                  |

### Architecture

```
Kafka (logs-raw)
    │
    ▼
LogConsumer (main loop)
    │
    ├── LogParser        JSON → Seq[LogEntry]
    │
    ├── LogStorageWriter (trait)
    │   ├── PostgresBatchWriter   JDBC batch insert
    │   └── ElasticsearchBatchWriter  ES Bulk API
    │
    └── DeadLetterQueue  Failed records → Kafka topic (logs-raw-dlq)
```

### Components

**`LogParser`** — Parses JSON arrays into `Seq[LogEntry]`. Handles both `camelCase` and `snake_case` field variants. Logs warnings for entries with missing required fields.

**`LogStorageWriter` trait** — Storage abstraction:
- `write(entries: Seq[LogEntry]): Either[String, Int]`
- `close(): Unit`

**`PostgresBatchWriter`** — JDBC batch insert via ScalikeJDBC's `SQL.batch()`. Uses `DB.localTx` for transactional safety. Configurable via env vars.

**`ElasticsearchBatchWriter`** — Uses Elasticsearch Bulk API (`/_bulk`). Auto-creates index with mapping on startup if it doesn't exist.

**`StorageWriterFactory`** — Creates writer(s) based on `STORAGE_WRITER` env var:
- `postgres` (default) — PostgresBatchWriter only
- `elasticsearch` / `es` — ElasticsearchBatchWriter only
- `both` / `all` — Both writers (dual-write)

In dual-write mode, a record goes to the DLQ only if **all** writers fail.

**`DeadLetterQueue`** — Publishes failed entries (with reason) to a separate `logs-raw-dlq` Kafka topic for later reprocessing.

### Environment Variables

| Variable              | Default                  | Description                     |
|-----------------------|--------------------------|---------------------------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`      | Kafka brokers                   |
| `DB_URL`              | `jdbc:postgresql://...`  | PostgreSQL JDBC URL             |
| `DB_USERNAME`         | `shubhamkudekar`         | DB user                         |
| `DB_PASSWORD`         | `""`                     | DB password                     |
| `BATCH_SIZE`          | `100`                    | Max records per poll            |
| `POLL_TIMEOUT_MS`     | `1000`                   | Kafka poll timeout              |
| `STORAGE_WRITER`      | `postgres`               | Writer mode (postgres/es/both)  |
| `ES_HOST`             | `localhost`              | Elasticsearch host              |
| `ES_PORT`             | `9200`                   | Elasticsearch port              |
| `ES_SCHEME`           | `http`                   | Elasticsearch scheme            |
| `ES_INDEX`            | `logs`                   | Elasticsearch index name        |

### Elasticsearch Index Mapping

```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "level":           { "type": "keyword" },
      "message":         { "type": "text" },
      "resourceId":      { "type": "keyword" },
      "timestamp":       { "type": "date" },
      "traceId":         { "type": "keyword" },
      "spanId":          { "type": "keyword" },
      "commit":          { "type": "keyword" },
      "parentResourceId":{ "type": "keyword" }
    }
  }
}
```

---

## Production Deployment (Docker)

### Services

| Service       | Image / Build                  | Port(s)        |
|---------------|--------------------------------|----------------|
| zookeeper     | confluentinc/cp-zookeeper:7.5  | 2181           |
| kafka         | confluentinc/cp-kafka:7.5      | 9092           |
| postgres      | postgres:16-alpine             | 5432           |
| elasticsearch | elasticsearch:8.10.2           | 9200, 9300     |
| backend       | `./log_ingestor_backend`       | 3000           |
| frontend      | `./log_ingestor_frontend`      | 3001 → 80     |
| consumer      | `./log_ingestor_consumer`      | —              |

### Data Flow

```
POST / (ingest) → Backend → Kafka (logs-raw) → Consumer → PostgreSQL + Elasticsearch
POST /api/filter (search) → Backend → PostgreSQL (or ES in future)
```

### Design Decisions

- **Batch ingestion (size 100)**: Optimizes throughput for bulk log inserts while keeping transactions manageable.
- **POST for search**: Using POST instead of GET allows complex filter payloads without URL length limitations.
- **Indexes on all searchable columns**: Ensures reasonable query performance as the log table grows.
- **CORS locked to port 3001**: Prevents other origins from calling the API in development.
- **Separate frontend/backend ports**: Clean separation; could be deployed independently or behind a reverse proxy.
- **Kafka buffer**: Decouples ingestion throughput from database write capacity. Backend returns 202 Accepted immediately after publishing to Kafka; consumer handles writes asynchronously.
- **MessageQueuePublisher trait**: Allows swapping Kafka for other message queues (Redis Pub/Sub, SQS, etc.) without changing controllers or services.
- **LogStorageWriter trait**: Allows adding or swapping storage backends (PostgreSQL → Elasticsearch → both) without changing the consumer main loop.
- **Dead Letter Queue**: Failed records are persisted to a DLQ Kafka topic for reprocessing, preventing data loss during transient failures.
- **Dual-write mode**: Consumer can write to both PostgreSQL and Elasticsearch simultaneously for gradual migration or redundancy.
- **Auto-created ES index**: Index with strict mapping is created on consumer startup if missing, keeping deployment simple.
- **Pre-built stage for Docker**: Avoids sbt/Maven Central rate limiting in Docker builds; host runs `sbt stage`, Docker copies artifacts only.
