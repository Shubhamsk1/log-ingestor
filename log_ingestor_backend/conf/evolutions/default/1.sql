# --- !Ups

CREATE TABLE logs (
  level VARCHAR(255),
  message VARCHAR(255),
  resource_id VARCHAR(255),
  timestamp VARCHAR(255),
  trace_id VARCHAR(255),
  span_id VARCHAR(255),
  commit VARCHAR(255),
  parent_resource_id VARCHAR(255)
);

# --- !Downs

DROP TABLE IF EXISTS logs;
