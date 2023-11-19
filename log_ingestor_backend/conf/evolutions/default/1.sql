# --- !Ups

CREATE TABLE logs (

  level TEXT,
  message TEXT,
  resource_id TEXT,
  timestamp timestamptz,
  trace_id TEXT,
  span_id TEXT,
  commit TEXT,
  parent_resource_id TEXT
);

# --- !Downs

DROP TABLE IF EXISTS logs;
