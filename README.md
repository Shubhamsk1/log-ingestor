# Log Ingestor

A production-grade log ingestion pipeline with Kafka buffering, dual PostgreSQL/Elasticsearch storage, and a React search UI.

```
POST / → Backend → Kafka → Consumer → PostgreSQL + Elasticsearch → Search via UI
```

## Architecture

```
Frontend (React) ──► Backend (Play/Scala) ──► Kafka ──► Consumer (Scala) ──► PostgreSQL
                        │                                                    ──► Elasticsearch
                        └── (search queries hit PostgreSQL directly)
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for full details.

## Stack

| Layer          | Technology                     |
|----------------|--------------------------------|
| Frontend       | React 18, TypeScript, nginx    |
| Backend        | Play Framework 3.0 (Scala 2.13)|
| Message Queue  | Apache Kafka 3.5 + Zookeeper   |
| Database       | PostgreSQL 16                  |
| Search Engine  | Elasticsearch 8.10 (optional)  |
| Consumer       | Standalone Scala app           |

## Quick Start (Local)

### Prerequisites

- Java 17+, sbt 1.7+
- Node.js 16+
- Docker & Docker Compose (for Kafka, Postgres, ES)

### Run the full stack

```bash
# 1. Start infrastructure
docker compose up -d postgres kafka elasticsearch

# 2. Build backend
cd log_ingestor_backend && sbt stage

# 3. Build consumer
cd ../log_ingestor_consumer && sbt stage

# 4. Start consumer
log_ingestor_consumer/target/universal/stage/bin/log_ingestor_consumer &

# 5. Start backend
log_ingestor_backend/target/universal/stage/bin/log_ingestor_backend &

# 6. Start frontend
cd ../log_ingestor_frontend && npm install && npm start
```

Open http://localhost:3001

### Docker (full stack)

```bash
# Build all services
cd log_ingestor_backend && sbt stage
cd ../log_ingestor_consumer && sbt stage

# Start everything
cd .. && docker compose up --build
```

## Deployment (Oracle Cloud + DuckDNS)

### 1. Provision the VM

Create an Oracle Cloud **VM.Standard.A1.Flex** (ARM, 4 OCPU, 24GB RAM) with Ubuntu 22.04. See [ARCHITECTURE.md](ARCHITECTURE.md#production-deployment-docker) for details.

### 2. Install Docker

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

### 3. Set up DuckDNS

Create a free domain at https://duckdns.org (e.g., `log-ingestor.duckdns.org`).

Set up the auto-update cron job:

```bash
mkdir -p ~/duckdns
cat > ~/duckdns/duck.sh << 'EOF'
#!/bin/bash
echo url="https://www.duckdns.org/update?domains=log-ingestor&token=YOUR_TOKEN&ip=" | curl -k -o ~/duckdns/duck.log -s -K -
EOF
chmod 700 ~/duckdns/duck.sh

crontab -l 2>/dev/null; echo "*/5 * * * * ~/duckdns/duck.sh >/dev/null 2>&1" | crontab -
```

### 4. Deploy the stack

```bash
git clone https://github.com/YOUR_USER/log-ingestor.git
cd log-ingestor

# Build artifacts on the VM
cd log_ingestor_backend && sbt stage
cd ../log_ingestor_consumer && sbt stage
cd ..

# Start all services
docker compose up --build -d
```

### 5. Configure the frontend

Set the API base URL in `log_ingestor_frontend/.env`:

```
REACT_APP_API_URL=https://log-ingestor.duckdns.org
```

Rebuild the frontend Docker image and restart.

### 6. Open the firewall

Open ports 80/443 in Oracle Cloud's security list for the DuckDNS domain. Frontend serves on port 80 behind nginx.

Visit `https://log-ingestor.duckdns.org` (optionally behind Cloudflare for SSL).

## API

### `POST /` — Ingest logs

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

Returns `202 Accepted` — logs are queued in Kafka.

### `POST /api/filter` — Search logs

```json
{ "query": "level", "level": "error" }
{ "query": "message", "search_string": "timeout" }
{ "query": "resource", "resource_id": "abc-123" }
{ "query": "timestamp", "fromTime": "2024-01-01T00:00:00.000Z", "tillTime": "2024-01-31T23:59:59.000Z" }
```

## Environment Variables

| Variable                | Default                        | Description                    |
|-------------------------|--------------------------------|--------------------------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`             | Kafka brokers                  |
| `DB_URL`                | `jdbc:postgresql://localhost:5432/logs_ingestor_db` | PostgreSQL JDBC URL |
| `DB_USERNAME`           | `shubhamkudekar`               | DB user                        |
| `DB_PASSWORD`           | `""`                           | DB password                    |
| `STORAGE_WRITER`        | `postgres`                     | `postgres`, `es`, or `both`    |
| `ES_HOST`               | `localhost`                    | Elasticsearch host             |
| `ES_PORT`               | `9200`                         | Elasticsearch port             |
| `CORS_ORIGINS`          | `["http://localhost:3001"]`    | Allowed CORS origins           |
