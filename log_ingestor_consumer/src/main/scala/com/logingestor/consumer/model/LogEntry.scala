package com.logingestor.consumer.model

case class LogEntry(
  level: String,
  message: String,
  resourceId: String,
  timestamp: String,
  traceId: String,
  spanId: String,
  commit: String,
  parentResourceId: Option[String]
)
