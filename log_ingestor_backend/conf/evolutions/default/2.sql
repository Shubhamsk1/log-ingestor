# --- !Ups

-- Add index on the "level" column
CREATE INDEX IF NOT EXISTS idx_level ON logs (level);

-- Add index on the "message" column
CREATE INDEX IF NOT EXISTS idx_message ON logs (message);

-- Add index on the "resourceId" column
CREATE INDEX IF NOT EXISTS idx_resourceId ON logs (resource_id);

-- Add index on the "timestamp" column
CREATE INDEX IF NOT EXISTS idx_timestamp ON logs (timestamp);

-- Add index on the "traceId" column
CREATE INDEX IF NOT EXISTS idx_traceId ON logs (trace_id);

-- Add index on the "spanId" column
CREATE INDEX IF NOT EXISTS idx_spanId ON logs (span_id);

-- Add index on the "commit" column
CREATE INDEX IF NOT EXISTS idx_commit ON logs (commit);

-- Add composite index on "level" and "timestamp" columns
CREATE INDEX IF NOT EXISTS idx_level_timestamp ON logs (level, timestamp);



# --- !Downs

DROP INDEX IF EXISTS idx_level_timestamp;

DROP INDEX IF EXISTS idx_commit;

DROP INDEX IF EXISTS idx_spanId;

DROP INDEX IF EXISTS idx_traceId;

DROP INDEX IF EXISTS idx_timestamp;

DROP INDEX IF EXISTS idx_message;

DROP INDEX IF EXISTS idx_level;

